package com.github.makewheels.cfffmpeg;

import cn.hutool.http.HttpUtil;

import java.io.File;

public class TranscodeHandler {
    private File workDir = new File(System.getenv("work_dir"));
    private File ffmpegFile;

    /**
     * 准备ffmpeg
     */
    private void prepareFFmpeg() {
        File packagesFolder = new File(workDir, "packages");
        File ffmpegFolder = new File(packagesFolder, "ffmpeg");
        ffmpegFile = new File(ffmpegFolder, "ffmpeg");
        if (!ffmpegFile.exists()) {
            String ffmpegUrl = "https://common-objects.oss-cn-beijing.aliyuncs.com" +
                    "/ffmpeg/linux-static-builds/amd64/5.0.1/ffmpeg-5.0.1-amd64-static/ffmpeg";
            HttpUtil.downloadFile(ffmpegUrl, ffmpegFile);
        }
    }

    public void start() {
        prepareFFmpeg();

    }
}
