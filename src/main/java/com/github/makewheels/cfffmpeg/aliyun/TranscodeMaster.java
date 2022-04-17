package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.github.makewheels.cfffmpeg.TranscodeHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TranscodeMaster implements HttpRequestHandler {
    private TranscodeHandler transcodeHandler = new TranscodeHandler();

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
        for (int i = 0; i < 20; i++) {
            int finalI = i;
            new Thread(() -> {
                System.out.println(finalI);
                HttpRequest get = HttpUtil.createGet("https://transcoe-worker-" +
                        "video-transcode-ystvacorwn.cn-beijing-vpc.fcapp.run");
                get.header("X-Fc-Invocation-Type", "Async");
                String requestId = get.execute().header("X-Fc-Request-Id");
                System.out.println(requestId);
            }).start();
        }
    }

}
