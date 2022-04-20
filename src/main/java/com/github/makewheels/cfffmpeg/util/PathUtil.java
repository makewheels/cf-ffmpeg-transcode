package com.github.makewheels.cfffmpeg.util;

import cn.hutool.core.io.FileUtil;

import java.io.File;

public class PathUtil {
    private static final File nasDir = new File(System.getenv("nas_dir"));
    private static final File workFolder = new File(nasDir, "video-transcode");
    private static final File ffmpeg = new File(nasDir, "ffmpeg/ffmpeg");
    private static final File ffprobe = new File(nasDir, "ffmpeg/ffprobe");
    private static File missionFolder;

    public static String getFFmpeg() {
        return ffmpeg.getAbsolutePath();
//        return "ffmpeg";
    }

    public static String getFFprobe() {
        return ffprobe.getAbsolutePath();
    }

    public static void initMissionFolder(String missionId) {
        missionFolder = new File(workFolder, missionId);
        //如果存在，先清空
        FileUtil.del(missionFolder);
        FileUtil.mkdir(missionFolder);
    }

    public static File getMissionFolder() {
        return missionFolder;
    }
}
