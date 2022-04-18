package com.github.makewheels.cfffmpeg.util;

import java.io.File;

public class PathUtil {
    private static File workDir = new File(System.getenv("work_dir"), "video-transcode");
    private static File ffmpeg = new File(workDir, "ffmpeg/ffmpeg");
    private static File ffprobe = new File(workDir, "ffmpeg/ffprobe");
    private static File missionFolder;

    public static String getFFmpeg() {
        return ffmpeg.getAbsolutePath();
    }

    public static String getFFprobe() {
        return ffprobe.getAbsolutePath();
    }

    public static void initMissionFolder(String missionId) {
        missionFolder = new File(workDir, "mission/" + missionId);
        File[] files = missionFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    public static File getMissionFolder() {
        return missionFolder;
    }
}
