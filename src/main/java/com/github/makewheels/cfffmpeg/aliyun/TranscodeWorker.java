package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.core.util.RuntimeUtil;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.github.makewheels.cfffmpeg.TranscodeHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TranscodeWorker implements HttpRequestHandler {
    private TranscodeHandler transcodeHandler = new TranscodeHandler();

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
        String exec = RuntimeUtil.execForStr("ffmpeg -i /mnt/video-transcode/piece-007.mp4 -c h264 /tmp/out"
                + System.currentTimeMillis() + ".mp4");
        System.out.println(exec);
    }
}
