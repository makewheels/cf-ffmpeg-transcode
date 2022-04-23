package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TimeZone;

public class AliyunClean implements StreamRequestHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        boolean deleteResult = FileUtil.del(new File(System.getenv("nas_dir"), "video-transcode"));
        System.out.println("deleteResult = " + deleteResult);
        IoUtil.writeUtf8(output, true, deleteResult);
    }
}
