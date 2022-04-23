package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;

public class AliyunClean implements StreamRequestHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        File videoTranscodeFolder = new File(System.getenv("nas_dir"), "video-transcode");
        List<File> files = FileUtil.loopFiles(videoTranscodeFolder);
        System.out.println("总数：" + files.size());
        for (File file : files) {
            String path = file.getAbsolutePath();
            System.out.println(file.delete() + " " + path);
        }
        IoUtil.writeUtf8(output, true, files.size());
    }
}
