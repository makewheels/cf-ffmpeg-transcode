package com.github.makewheels.cfffmpeg.download;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DownloadController {

    public void getFileWithThreadPool(String urlLocation, String filePath, int poolLength)   {
        long start;
        int length;
        URL url;
        try {
            url = new URL(urlLocation);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        URLConnection urlConnection;
        try {
            urlConnection = url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //文件长度
        length = urlConnection.getContentLength();
        System.out.println(length);
        for (int i = 0; i < poolLength; i++) {
            start = (long) i * length / poolLength;
            long end = (long) (i + 1) * length / poolLength - 1;
            System.out.println(start + "---------------" + end);

            Thread thread = new Thread(new FileDownload(urlLocation, filePath, start, end));
            thread.setName("线程" + (i + 1));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DownloadController downloadController = new DownloadController();
        downloadController.getFileWithThreadPool(
                "https://video-2022-prod.oss-cn-beijing.aliyuncs.com/test/aliyun/slowres-00001.ts?Expires=1650358321&OSSAccessKeyId=TMP.3Ki4bagQ4vxrwtLM1JgRA42nAjaVJq4jd4LbVVokkaUnZmhCnWPKiv82TcP8PHEjFmh9NUQZN5cKb7xsYBasW6cyhMWJyC&Signature=LHfE920%2BJnrhDTFuYZJk%2B5KbtxM%3D",
                "D:\\BaiduNetdiskDownload\\demo-src-2.mp4", 4);
        System.out.println("main");
    }

}

