package com.github.makewheels.cfffmpeg.etc;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

public class TranscodeHandler {
    private File workDir = new File(System.getenv("work_dir"));
    private File ffmpegFile;
    private File transcodeFolder;
    private File inputFile;
    private File outputFolder;
    private File m3u8File;

    private String provider;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object contextObject;
    private JSONObject body;
    private String bucket;
    private String endpoint;
    private String inputKey;
    private String m3u8Key;
    private String accessKeyId;
    private String accessKeySecret;

    private OSS ossClient;
    private String missionId;

    private String videoId;

    /**
     * 准备ffmpeg
     */
    private void prepareArgs(String provider, HttpServletRequest request, HttpServletResponse response,
                             Object contextObject) throws IOException {
        this.provider = provider;
        this.request = request;
        this.response = response;
        this.contextObject = contextObject;

        body = JSON.parseObject(IoUtil.readUtf8(request.getInputStream()));
        bucket = body.getString("bucket");
        endpoint = body.getString("endpoint");
        inputKey = body.getString("inputKey");
        m3u8Key = body.getString("m3u8Key");
        accessKeyId = System.getenv("s3_accessKeyId");
        accessKeySecret = System.getenv("s3_accessKeySecret");

        videoId = body.getString("videoId");
        missionId = body.getString("missionId");
        transcodeFolder = new File(workDir, missionId);
        inputFile = new File(transcodeFolder, FileNameUtil.getName(inputKey));
        outputFolder = new File(transcodeFolder, "out");
        m3u8File = new File(outputFolder, "index.m3u8");

        FileUtil.mkdir(transcodeFolder);
        FileUtil.mkdir(outputFolder);
    }

    /**
     * 下载原始文件
     */
    private void prepareInputFile() {
        if (provider.equals("aliyun")) {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            System.out.println("下载文件：" + inputFile.getAbsolutePath());
            ossClient.getObject(new GetObjectRequest(bucket, inputKey), inputFile);
        }
    }

    private void executeAndPrint(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行转码
     */
    private void transcodeM3u8() {
        String cmd = ffmpegFile.getAbsolutePath() + " -i " + inputFile.getAbsolutePath() + " -codec copy " +
                "-vbsf h264_mp4toannexb -flags +global_header -map 0 -f segment -segment_list " +
                outputFolder.getAbsolutePath() + "/index.m3u8 -segment_time 1 " + outputFolder.getAbsolutePath()
                + "/%04d.ts";
        System.out.println(cmd);
        executeAndPrint(cmd);
    }

    /**
     * 上传转码结果到对象存储
     */
    private void uploadFiles() {
        String parentKey = m3u8Key.substring(0, m3u8Key.lastIndexOf("/") + 1);
        List<File> files = FileUtil.loopFiles(outputFolder);
        for (File file : files) {
            String key = parentKey + file.getName();
            System.out.println("上传文件：key = " + key);
            ossClient.putObject(bucket, key, file);
        }
    }

    private void deleteAllFiles() {
        System.out.println("删除所有文件 " + transcodeFolder.getAbsolutePath());
        FileUtil.del(transcodeFolder);
    }

    /**
     * 回调
     */
    private void callback() {
        String callbackUrl = body.getString("callbackUrl");
        if (StringUtils.isNotEmpty(callbackUrl)) {
            HttpUtil.get(callbackUrl);
        }
    }

    /**
     * 从这里开始
     *
     * @param provider
     * @param request
     * @param response
     * @param contextObject
     */
    public void start(String provider, HttpServletRequest request, HttpServletResponse response,
                      Object contextObject) throws IOException {

        Context context = (Context) contextObject;
        String str = "我是云函数，我收到任务了，requestId = " + context.getRequestId();
        IoUtil.writeUtf8(response.getOutputStream(), true, str);

        //创建子线程执行，先给前端返回结果
//        Thread thread = new Thread(() -> {
        try {
            prepareArgs(provider, request, response, contextObject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        prepareInputFile();
        transcodeM3u8();
        uploadFiles();
        deleteAllFiles();
        callback();
//        });
//        thread.start();

    }

}
