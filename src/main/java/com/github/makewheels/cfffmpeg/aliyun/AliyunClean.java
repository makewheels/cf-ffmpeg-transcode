package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.github.makewheels.cfffmpeg.functions.ffprobe.FFprobe;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

public class AliyunClean implements HttpRequestHandler {
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        boolean deleteResult = FileUtil.del(new File(System.getenv("nas_dir"), "video-transcode"));
        System.out.println("deleteResult = " + deleteResult);
        IoUtil.writeUtf8(response.getOutputStream(), true, deleteResult);
    }

}
