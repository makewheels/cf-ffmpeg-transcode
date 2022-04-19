package com.github.makewheels.cfffmpeg.download;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FileDownload implements Runnable {

    //下载路径
    private final String urlLocation;
    //下载完存放路径
    private final String filePath;
    //文件开始地址
    private final long start;
    //文件结束地址
    private final long end;

    public FileDownload(String urlLocation, String filePath, long start, long end) {
        this.urlLocation = urlLocation;
        this.filePath = filePath;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        InputStream is = null;
        RandomAccessFile out = null;
        try {
            //获取下载的部分
            URL url = new URL(urlLocation);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);

            File file = new File(filePath);
            out = new RandomAccessFile(file, "rw");
            out.seek(start);

            is = conn.getInputStream();
            byte[] bytes = new byte[1024];
            int l;
            while ((l = is.read(bytes)) != -1) {
                out.write(bytes, 0, l);
            }
            System.out.println(Thread.currentThread().getName() + "完成下载！");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}

