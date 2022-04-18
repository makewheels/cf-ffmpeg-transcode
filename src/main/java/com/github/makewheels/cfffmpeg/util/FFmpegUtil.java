package com.github.makewheels.cfffmpeg.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RuntimeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FFmpegUtil {
    private static final String ffmpeg = PathUtil.getFFmpeg();

    private static String run(String cmd) {
        log.info(cmd);
        return RuntimeUtil.execForStr(cmd);
    }

    public static void extractVideo(File src, File dest) {
        FileUtil.mkParentDirs(dest);
        run(ffmpeg + " -i \"" + src.getAbsolutePath() + "\" -c copy -an \"" + dest.getAbsolutePath() + "\"");
    }

    public static void extractAudio(File src, File dest) {
        FileUtil.mkParentDirs(dest);
        run(ffmpeg + " -i \"" + src.getAbsolutePath() + "\" -c copy -vn \"" + dest.getAbsolutePath() + "\"");
    }

    /**
     * 按时长分割
     */
    public static File splitByTime(File src, File destFolder, int segmentTime) {
        FileUtil.mkdir(destFolder);
        File listFile = new File(destFolder, "pieces.list");
        run(ffmpeg + " -i \"" + src.getAbsolutePath() + "\" -c copy " +
                "-f segment -segment_time " + segmentTime
                + " -segment_list \"" + listFile.getAbsolutePath() + "\" "
                + destFolder.getAbsolutePath() + "/piece-%04d." + FileNameUtil.extName(src) + "\"");
        return listFile;
    }

    /**
     * 合并碎片，根据ffmpeg文件列表格式：
     * file 'filename.mp4'
     *
     * @param ffmpegFileList
     * @param dest
     */
    private static void mergeSegments(File ffmpegFileList, File dest) {
        FileUtil.mkParentDirs(dest);
        run(ffmpeg + " -i \"" + ffmpegFileList.getAbsolutePath() + "\" " +
                "-f concat -c copy -fflags +genpts " +
                "\"" + dest.getAbsolutePath() + "\""
        );
    }

    /**
     * 合并碎片到一个文件
     * 只需传入文件列表，自动生成ffmpeg所需要的文件列表格式，执行合并
     *
     * @param fileList
     * @param dest
     */
    public static void mergeSegments(List<File> fileList, File dest) {
        if (CollectionUtil.isEmpty(fileList)) return;
        FileUtil.mkParentDirs(dest);
        List<String> lines = new ArrayList<>(fileList.size());
        for (File file : fileList) {
            lines.add("file '" + file.getAbsolutePath() + "'");
        }
        File ffmpegFileList = new File(fileList.get(0), "merge-pieces-" + IdUtil.simpleUUID() + ".list");
        FileUtil.writeUtf8Lines(lines, ffmpegFileList);
        mergeSegments(ffmpegFileList, dest);
    }

    /**
     * 转m3u8
     */
    public static File createHls(File src, File destFolder, String prefix, int hlsTime) {
        FileUtil.mkdir(destFolder);
        File m3u8 = new File(destFolder, prefix + ".m3u8");
        run(ffmpeg + " -i \"" + src.getAbsolutePath() + "\" -c copy -hls_list_size 0 " +
                "-hls_time " + hlsTime
                + " -hls_segment_filename \"" + destFolder.getAbsolutePath() + "/" + prefix + "-%05d.ts\" "
                + "\"" + m3u8.getAbsolutePath() + "\""
        );
        return m3u8;
    }

    /**
     * 合并音视频到一个文件
     */
    public static void mergeVideoAndAudio(File video, File audio, File dest) {
        FileUtil.mkParentDirs(dest);
        run(ffmpeg
                + " -i \"" + video.getAbsolutePath() + "\" "
                + " -i \"" + audio.getAbsolutePath() + "\" " +
                "-c copy \"" + dest.getAbsolutePath() + "\""
        );
    }
}
