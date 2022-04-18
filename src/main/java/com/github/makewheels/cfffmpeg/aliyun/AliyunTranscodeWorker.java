package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.github.makewheels.cfffmpeg.TranscodeHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class AliyunTranscodeWorker implements HttpRequestHandler {
    private TranscodeHandler transcodeHandler = new TranscodeHandler();

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
        new Thread(() -> {
            //拷贝文件 video-original.mp4
            File input = new File("/mnt/video-transcode/piece-007.mp4");
            File tempFile = new File("/tmp/temp-file-" + IdUtil.simpleUUID() + ".mp4");
            System.out.println("开始从nas拷贝到本机tmp目录，文件大小：" + input.length());
            long start = System.currentTimeMillis();
            FileUtil.copyFile(input, tempFile);
            System.out.println("耗时：" + (System.currentTimeMillis() - start));

            File output = new File("/mnt/video-transcode/piece-007.mp4");

//            String exec = RuntimeUtil.execForStr("ffmpeg -i " + input + " -c h264 " +
//                    "/tmp/out-" + System.currentTimeMillis() + ".mp4");
//            System.out.println(exec);
        }).start();
        try {
            Thread.sleep(6 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String str = "我是worker，我收到任务了，requestId = " + context.getRequestId();
        IoUtil.writeUtf8(response.getOutputStream(), true, str);
    }
}
