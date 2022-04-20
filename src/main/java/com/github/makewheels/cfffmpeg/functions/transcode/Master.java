package com.github.makewheels.cfffmpeg.functions.transcode;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

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

    private JSONObject meta;

    /**
     * 开始执行
     */
    public String start(JSONObject body) {
        log.info("Master.start");
        init(body);
        handleTranscode();
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
     * 视频是否需要改分辨率
     *
     * @param meta
     * @return
     */
    private boolean isNeedChangeVideoResolution(JSONObject meta) {
        JSONObject videoSteam = FFprobeUtil.getVideoSteam(meta);
        return videoSteam.getInteger("width") * videoSteam.getInteger("height") > (width * height);
    }

    /**
     * 视频是否需要改codec
     *
     * @param meta
     * @return
     */
    private boolean isNeedChangeVideoCodec(JSONObject meta) {
        JSONObject videoSteam = FFprobeUtil.getVideoSteam(meta);
        return !videoSteam.getString("codec_name").equals(videoCodec);
    }

    /**
     * 是否需要转码视频
     *
     * @param meta
     * @return
     */
    private boolean isNeedTranscodeVideo(JSONObject meta) {
        return isNeedChangeVideoCodec(meta) && isNeedChangeVideoResolution(meta);
    }

    /**
     * 音频是否需要改codec
     *
     * @param meta
     * @return
     */
    private boolean isNeedChangeAudioCodec(JSONObject meta) {
        JSONObject videoSteam = FFprobeUtil.getVideoSteam(meta);
        return !videoSteam.getString("codec_name").equals(videoCodec);
    }

    /**
     * 是否需要转码音频
     *
     * @param meta
     * @return
     */
    private boolean isNeedTranscodeAudio(JSONObject meta) {
        return isNeedChangeAudioCodec(meta);
    }

    /**
     * 判断是否需要启动worker并发转码
     * 目前来说，我要改的东西只有：分辨率，音频、视频的codec
     */
    private boolean isNeedTranscode(JSONObject meta) {
        int bitRate = Integer.parseInt(meta.getJSONObject("format").getString("bit_rate"));
        return isNeedTranscodeAudio(meta) || isNeedTranscodeVideo(meta);
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

    private void handleTranscode() {
        downloadFile();

        //ffprobe读取源文件信息
        meta = FFprobeUtil.getMeta(inputFile);
        //判断是否需要启动worker分片转码
        File finalFile = inputFile;
        boolean isNeedWorkers = isNeedTranscode(meta);
        log.info("isNeedWorkers = " + isNeedWorkers);
        if (isNeedWorkers) {
            try {
                finalFile = runVideoAndAudioTranscode();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                        //每次上传前，先抬头看一下，我是不是已经停了
                        if (!running) return;
                        //所以判断如果文件不存在则跳过
                        if (!file.exists()) {
                            continue;
                        }
                        String filename = file.getName();
                        String shortName = filename.substring(filename.length() - 8);
                        s3Service.putObject(outputDir + "/" + filename, file);
                        boolean delete = file.delete();
//                        log.info("子线程上传：" + shortName + " 删除：" + delete);
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
        File[] filesArray = destFolder.listFiles();
        if (filesArray != null && filesArray.length != 0) {
            List<File> files = new ArrayList<>(Arrays.asList(filesArray));
            log.info("主线程：扫描文件结果，总数：" + files.size());
            //这时可能子线程还在上传，主线程从尾部开始传，它俩不冲突
            Collections.reverse(files);
            for (File file : files) {
                //我终究还是加锁了，避免出现一种情况
                //这里文件是存在的，但是s3 sdk上传的过程中，文件被其它线程删掉了，那会抛异常
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
     * 等待线程池的任务都执行结束
     *
     * @param executorService
     * @param futureTasks
     */
    private <T> void waitComplete(ExecutorService executorService, List<FutureTask<T>> futureTasks) {
        while (true) {
            boolean isAllFinish = true;
            for (FutureTask<T> futureTask : futureTasks) {
                if (futureTask.isDone()) {
//                    try {
//                        log.info("worker某不重要结果长度："
//                                + FileUtil.readableFileSize(futureTask.get().toString().length()));
//                    } catch (InterruptedException | ExecutionException e) {
//                        throw new RuntimeException(e);
//                    }
                } else {
                    isAllFinish = false;
                    break;
                }
            }
            if (isAllFinish) {
                executorService.shutdown();
                break;
            }
        }
    }

    /**
     * 真正开始执行转码，启动并发分片
     */
    private File runVideoAndAudioTranscode() throws Exception {
        FutureTask<File> futureTask1 = new FutureTask<>(this::handleVideo);
        FutureTask<File> futureTask2 = new FutureTask<>(this::handleAudio);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<FutureTask<File>> futureTasks = ListUtil.toList(futureTask1, futureTask2);
        futureTasks.forEach(executorService::submit);

        //子线程等待音视频两个线程结果
        waitComplete(executorService, futureTasks);

        File video = futureTask1.get();
        File audio = futureTask2.get();

        //合并音视频
        File finalFile = new File(missionFolder, "final/final.mp4");
        FFmpegUtil.mergeVideoAndAudio(video, audio, finalFile);
        return finalFile;
    }

    /**
     * 处理视频部分
     */
    private File handleVideo() {
        log.info("开始分离video");
        FFmpegUtil.extractVideo(inputFile, extractVideo);
        log.info("分离video完成 " + extractVideo.getAbsolutePath());

        //判断是否需要转码
        if (!isNeedChangeVideoCodec(meta)) return extractVideo;

        File transcodeFolder = new File(missionFolder, "handle-video");
        File segmentsFolder = new File(transcodeFolder, "original-segments");
        File transcodeResultsFolder = new File(transcodeFolder, "transcode-segments");
        log.info("视频转码：开始分片segments");
        FFmpegUtil.splitToSegments(extractVideo, segmentsFolder, 8);

        List<File> segments = FileUtil.loopFiles(segmentsFolder, file -> !file.getName().endsWith(".list"));
        int segmentAmount = segments.size();
        log.info("视频转码：分片segments完成，总数：" + segmentAmount);

        log.info("视频转码：开始并发启动容器");
        ExecutorService executorService = Executors.newFixedThreadPool(segmentAmount);
        List<FutureTask<String>> futureTasks = new ArrayList<>(segmentAmount);
        for (File segment : segments) {
            JSONObject request = new JSONObject();
            //如果要改分辨率
            String resolutionCmd = "";
            if (isNeedChangeVideoResolution(meta)) resolutionCmd = "-vf scale=-2:720 ";
            String codecCmd;
            //如果要改codec
            if (isNeedChangeVideoCodec(meta)) {
                codecCmd = "-c:v " + videoCodec + " ";
            } else {
                codecCmd = "-c copy ";
            }
            FileUtil.mkdir(transcodeResultsFolder);
            String cmd = PathUtil.getFFmpeg() + " -i " + segment.getAbsolutePath() + " " + codecCmd
                    + resolutionCmd + transcodeResultsFolder.getAbsolutePath() + "/" + segment.getName();
            request.put("cmd", cmd);
            FutureTask<String> futureTask = new FutureTask<>(() ->
                    HttpUtil.post("https://transcoe-worker-video-transcode"
                            + "-ystvacorwn.cn-beijing-vpc.fcapp.run", request.toJSONString())
            );
            executorService.submit(futureTask);
            futureTasks.add(futureTask);
        }
        log.info("视频转码：启动容器完成");

        //等待容器所有转码任务都完成
        waitComplete(executorService, futureTasks);

        log.info("视频转码：所有容器转码任务都完成了");
        log.info("视频转码：开始合并分片");
        File finalVideo = new File(transcodeFolder, "final/final." + ext);
        FFmpegUtil.mergeSegments(FileUtil.loopFiles(transcodeResultsFolder), finalVideo);
        log.info("视频转码：合并分片完成，视频部分结束");
        return finalVideo;
    }

    /**
     * 处理音频部分
     */
    private File handleAudio() {
        log.info("开始分离audio");
        FFmpegUtil.extractAudio(inputFile, extractAudio);
        log.info("分离audio完成 " + extractAudio.getAbsolutePath());

        //判断是否需要转码
        if (!isNeedTranscodeAudio(meta)) return extractAudio;

        File transcodeFolder = new File(missionFolder, "handle-audio");
        File segmentsFolder = new File(transcodeFolder, "original-segments");
        File transcodeResultsFolder = new File(transcodeFolder, "transcode-segments");
        log.info("音频转码：开始分片");
        FFmpegUtil.splitToSegments(extractAudio, segmentsFolder, 128);

        List<File> segments = FileUtil.loopFiles(segmentsFolder, file -> !file.getName().endsWith(".list"));
        int segmentAmount = segments.size();
        log.info("音频转码：分片segments完成，总数：" + segmentAmount);

        log.info("音频转码：开始并发启动容器");
        ExecutorService executorService = Executors.newFixedThreadPool(segmentAmount);
        List<FutureTask<String>> futureTasks = new ArrayList<>(segmentAmount);
        for (File segment : segments) {
            JSONObject request = new JSONObject();
            String codecCmd;
            //如果要改codec
            if (isNeedChangeAudioCodec(meta)) {
                codecCmd = "-c:a " + audioCodec + " ";
            } else {
                codecCmd = "-c copy ";
            }
            FileUtil.mkdir(transcodeResultsFolder);
            String cmd = PathUtil.getFFmpeg() + " -i " + segment.getAbsolutePath() + " " + codecCmd
                    + transcodeResultsFolder.getAbsolutePath() + "/" + segment.getName();
//            log.info(cmd);
            request.put("cmd", cmd);
            FutureTask<String> futureTask = new FutureTask<>(() ->
                    HttpUtil.post("https://transcoe-worker-video-transcode"
                            + "-ystvacorwn.cn-beijing-vpc.fcapp.run", request.toJSONString())
            );
            executorService.submit(futureTask);
            futureTasks.add(futureTask);
        }
        log.info("音频转码：启动容器完成");

        //等待容器所有转码任务都完成
        waitComplete(executorService, futureTasks);

        log.info("音频转码：所有容器转码任务都完成了");
        log.info("音频转码：开始合并分片");
        File finalAudio = new File(transcodeFolder, "final/final." + ext);
        FFmpegUtil.mergeSegments(FileUtil.loopFiles(transcodeResultsFolder), finalAudio);
        log.info("音频转码：合并分片完成，音频部分结束");
        return finalAudio;
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
//        FileUtil.del(missionFolder);
    }

}
