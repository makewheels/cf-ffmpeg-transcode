package com.github.makewheels.cfffmpeg;

import cn.hutool.core.io.IoUtil;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class AliyunCloudFunction implements HttpRequestHandler {
    private TranscodeHandler transcodeHandler = new TranscodeHandler();

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
//        transcodeHandler.start();
        String body = IoUtil.readUtf8(request.getInputStream());
        System.out.println(body);

        String requestPath = (String) request.getAttribute("FC_REQUEST_PATH");
        String requestURI = (String) request.getAttribute("FC_REQUEST_URI");
        String requestClientIP = (String) request.getAttribute("FC_REQUEST_CLIENT_IP");

        response.setStatus(200);

        OutputStream out = response.getOutputStream();
        out.write((String.format("Path: %s\n Uri: %s\n IP: %s\n", requestPath,
                requestURI, requestClientIP)).getBytes());
        out.flush();
        out.close();
    }
}
