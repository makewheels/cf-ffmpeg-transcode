package com.github.makewheels.cfffmpeg.transcode;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.makewheels.cfffmpeg.s3.S3Service;
import com.github.makewheels.cfffmpeg.util.FFmpegUtil;
import com.github.makewheels.cfffmpeg.util.FFprobeUtil;
import com.github.makewheels.cfffmpeg.util.PathUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

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
    private String callbackUrl;

    private File missionFolder;
    private File inputFile;
    private String ext;
    private File extractAudio;
    private File extractVideo;

    /**
     * 开始执行
     */
    public String start(JSONObject body) {
        log.info("Master.start");
        init(body);
        transcode();
        onFinish();
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
        callbackUrl = body.getString("callbackUrl");

        PathUtil.initMissionFolder(missionId);
        missionFolder = PathUtil.getMissionFolder();

        ext = "mp4";
        inputFile = new File(missionFolder, "original/" + FileNameUtil.getName(inputKey));
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

    private void downloadFile() {
        //从对象存储下载原始文件
        log.info("开始从对象存储下载源文件 inputKey = " + inputKey);
        FileUtil.mkParentDirs(inputFile);
        String url = s3Service.signGetUrl(inputKey);
        System.out.println(url);
//        System.out.println(RuntimeUtil.execForStr("
//        sed -i -E 's/(deb|security).debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list"));
//        System.out.println(RuntimeUtil.execForStr("apt update"));
//        System.out.println(RuntimeUtil.execForStr("apt install axel"));
//        System.out.println(RuntimeUtil.execForStr("/usr/bin/axel -n 4 " + url
//        + " -o " + inputFile.getAbsolutePath()));
        HttpUtil.downloadFile(url, inputFile);
        log.info("对象存储下载完成 inputFile = " + inputFile.getAbsolutePath());
    }

    private void transcode() {
        downloadFile();

        //ffprobe读取源文件信息
        JSONObject meta = FFprobeUtil.getMeta(inputFile);
        //判断是否需要启动worker分片转码
        File finalFile = inputFile;
        boolean isNeedWorkers = isNeedWorkers(meta);
        log.info("isNeedWorkers = " + isNeedWorkers);
        if (isNeedWorkers) {
            finalFile = runWorkers();
        }
        //转hls
        createAndUploadHls(finalFile);
    }

    private final Object uploadLock = new Object();

    class Uploader extends Thread {
        private boolean running = true;
        private final File destFolder;

        public Uploader(File destFolder) {
            this.destFolder = destFolder;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void run() {
            while (running) {
                ThreadUtil.sleep(4);
                //扫描ts文件
                File[] filesArray = destFolder.listFiles(file -> file.getName().endsWith(".ts"));
                if (filesArray == null || filesArray.length == 0) continue;

                List<File> files = new ArrayList<>(Arrays.asList(filesArray));
                files.sort(Comparator.comparing(File::getName));
                //安全起见，最后一个文件可能ffmpeg还在写入，跳过
                files.remove(files.size() - 1);
                if (CollectionUtil.isEmpty(files)) continue;
                log.info("子线程扫描文件列表，总数：" + files.size());
                //上传
                for (File file : files) {
                    //可能子线程这一批任务执行的慢了，那主线程已将上传完，并且删除了文件
                    synchronized (uploadLock) {
                        //所以判断如果文件不存在则跳过
                        if (!file.exists()) {
                            continue;
                        }
                        String filename = file.getName();
                        String shortName = filename.substring(filename.length() - 8);
                        s3Service.putObject(outputDir + "/" + filename, file);
                        boolean delete = file.delete();
                        log.info("子线程上传：" + shortName + " 删除：" + delete);
                    }
                }
            }
        }
    }

    /**
     * 转码m3u8
     * 原来是串行，先转出很多ts碎片，再loopFiles，上传对象存储
     * 现在改成并行，主线程运行ffmpeg转hls，子线程扫描destFolder上传ts碎片
     */
    private void createAndUploadHls(File src) {
        log.info("开始转码hls");
        File destFolder = new File(missionFolder, "hls");
        //启动子线程上传对象存储
        Uploader uploader = new Uploader(destFolder);
        uploader.start();
        //开始转码
        FFmpegUtil.createHls(src, destFolder, transcodeId, 1);
        //转码完成，停止子线程，主线程继续上传剩下的
        uploader.setRunning(false);
        log.info("主线程：转码完毕，已将子线程停止");
        //子线程可能在正在上传和删除，遍历上传要先判断文件是否存在
        File[] filesArray = destFolder.listFiles(file -> file.getName().endsWith(".ts"));
        if (filesArray != null && filesArray.length != 0) {
            List<File> files = new ArrayList<>(Arrays.asList(filesArray));
            log.info("主线程：扫描文件结果，总数：" + files.size());
            //这时可能子线程还在上传，主线程从尾部开始传，它俩不冲突
            Collections.reverse(files);
            for (File file : files) {
                //我终究还是加锁了，避免出现一种情况
                //这里文件是存在的，但是s3 sdk上传的过程中，文件被其它线程删掉了，那还会抛异常
                synchronized (uploadLock) {
                    if (!file.exists()) {
                        log.info("主线程：文件不存在，跳过：" + file.getName());
                        continue;
                    }
                    s3Service.putObject(outputDir + "/" + file.getName(), file);
                    log.info("主线程：上传：" + file.getName());
                }
            }
        }
        log.info("转码hls完成");
    }

    /**
     * 真正开始执行转码，启动并发分片
     */
    private File runWorkers() {
        log.info("开始分离audio");
        FFmpegUtil.extractAudio(inputFile, extractAudio);
        log.info("分离audio完成 " + extractAudio.getAbsolutePath());

        log.info("开始分离video");
        FFmpegUtil.extractVideo(inputFile, extractVideo);
        log.info("分离video完成 " + extractVideo.getAbsolutePath());

        return inputFile;
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

    /**
     * 在转码完成时
     */
    private void onFinish() {
        if (callbackUrl != null) {
            log.info("callback开始");
            HttpUtil.get(callbackUrl);
            log.info("callback结束");
        }
        FileUtil.del(missionFolder);
    }

}
