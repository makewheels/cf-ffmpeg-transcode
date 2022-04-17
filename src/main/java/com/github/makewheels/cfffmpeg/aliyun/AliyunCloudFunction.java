package com.github.makewheels.cfffmpeg.aliyun;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.github.makewheels.cfffmpeg.TranscodeHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AliyunCloudFunction implements HttpRequestHandler {
    private TranscodeHandler transcodeHandler = new TranscodeHandler();
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
//        transcodeHandler.start("aliyun", request, response, context);
        System.out.println("fajwoijwoeig" + System.currentTimeMillis());
    }
}
