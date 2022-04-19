package com.github.makewheels.cfffmpeg.transcode;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.makewheels.cfffmpeg.s3.S3Service;
import com.github.makewheels.cfffmpeg.util.FFmpegUtil;
import com.github.makewheels.cfffmpeg.util.FFprobeUtil;
import com.github.makewheels.cfffmpeg.util.PathUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j

public class Master {
    private final S3Service s3Service = new S3Service();
    private String videoId;
    private String transcodeId;
    private String missionId;
    private String inputKey;
    private String outputDir;
    private int width;
    private int height;
    private String videoCodec;
    private String audioCodec;
    private String quality;

    private File missionFolder;
    private File inputFile;
    private String ext;
    private File extractAudio;
    private File extractVideo;

    private final JSONObject cost = new JSONObject();

    /**
     * 开始执行
     */
    public String start(JSONObject body) {
        log.info("Master.start");
        init(body);
        transcode();
        log.info("Master.end");
        return "transcode master return";
    }

    /**
     * 初始化
     */
    private void init(JSONObject body) {
        s3Service.init(body.getString("bucket"), body.getString("region"), body.getString("endpoint"));
        inputKey = body.getString("inputKey");
        outputDir = body.getString("outputDir");
        videoId = body.getString("videoId");
        transcodeId = body.getString("transcodeId");
        missionId = body.getString("missionId");
        width = body.getInteger("width");
        height = body.getInteger("height");
        videoCodec = body.getString("videoCodec");
        audioCodec = body.getString("audioCodec");
        quality = body.getString("quality");

        PathUtil.initMissionFolder(missionId);
        missionFolder = PathUtil.getMissionFolder();

        ext = "mp4";
        inputFile = new File(missionFolder, "original/" + FileNameUtil.getName(inputKey));
        inputFile = new File("/tmp/original/" + FileNameUtil.getName(inputKey));
        extractAudio = new File(missionFolder, "extract/" + "audio." + ext);
        extractVideo = new File(missionFolder, "extract/" + "video." + ext);
    }

    /**
     * 判断是否需要启动worker并发转码
     * 目前来说，我要改的东西只有：分辨率，音频、视频的codec
     */
    private boolean isNeedWorkers(JSONObject meta) {
        meta.getJSONObject("format").getString("bit_rate");
        JSONObject audioSteam = FFprobeUtil.getAudioSteam(meta);
        JSONObject videoSteam = FFprobeUtil.getVideoSteam(meta);
        //如果codec不一致，那就需要转码
        if (!audioSteam.getString("codec_name").equals(audioCodec)) {
            return true;
        }
        if (!videoSteam.getString("codec_name").equals(videoCodec)) {
            return true;
        }
        //如果源视频分辨率，比目标分辨率大，就改
        if (videoSteam.getInteger("width") * videoSteam.getInteger("height") > (width * height)) {
            return true;
        }
        return false;
    }

    private void transcode() {
        //从对象存储下载原始文件
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("开始从对象存储下载源文件 inputKey = " + inputKey);
        s3Service.download(inputKey, inputFile);
        log.info("对象存储下载完成 inputFile = " + inputFile.getAbsolutePath());
        stopWatch.stop();
        cost.put("downloadOriginalFile", stopWatch.getLastTaskTimeMillis());

        //ffprobe读取源文件信息
        JSONObject meta = FFprobeUtil.getMeta(inputFile);
        //判断是否需要启动worker分片转码
        File finalFile = inputFile;
        boolean isNeedWorkers = isNeedWorkers(meta);
        log.info("isNeedWorkers = " + isNeedWorkers);
        if (isNeedWorkers) {
//            finalFile = runWorkers();
        }
        //转hls
        File destFolder = new File(missionFolder, "hls");
        log.info("开始转码hls");
        FFmpegUtil.createHls(finalFile, destFolder, transcodeId, 1);
        log.info("转码hls完成");
        uploadHls(destFolder);
    }

    /**
     * 上传m3u8碎片
     */
    private void uploadHls(File hlsFolder) {
        List<File> files = FileUtil.loopFiles(hlsFolder);
        log.info("开始上传hls到对象存储，数量 = " + files.size());
        for (File file : files) {
            s3Service.putObject(outputDir + "/" + file.getName(), file);
        }
        log.info("上传对象存储完成");
    }

    /**
     * 真正开始执行转码，启动并发分片
     */
    private File runWorkers() {
        log.info("分离audio");
        FFmpegUtil.extractAudio(inputFile, extractAudio);
        log.info("分离audio完成 " + extractAudio.getAbsolutePath());

        log.info("分离video");
        FFmpegUtil.extractVideo(inputFile, extractVideo);
        log.info("分离video完成 " + extractVideo.getAbsolutePath());

        return new File("");
    }

    /**
     * 处理视频部分
     */
    private void handleVideo() {

    }

    /**
     * 处理音频部分
     */
    private void handleAudio() {

    }

}
