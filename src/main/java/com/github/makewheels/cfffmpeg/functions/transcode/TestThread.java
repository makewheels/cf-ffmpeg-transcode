package com.github.makewheels.cfffmpeg.functions.transcode;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class TestThread {
    public static void main(String[] args) throws InterruptedException {
        FutureTask<File> futureTask1 = new FutureTask<>(new Callable<File>() {
            @Override
            public File call() throws Exception {
                return null;
            }
        });
        FutureTask<File> futureTask2 = new FutureTask<>(null);
//        for (FutureTask<String> futureTask : null) {
//            try {
//                System.out.println("worker某不重要结果长度：" + FileUtil.readableFileSize(futureTask.get().length()));
//            } catch (InterruptedException | ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        }


        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                ThreadUtil.sleep(40);
                System.out.println(Thread.currentThread().getName());
            });
            thread.start();
            thread.join();
        }
        System.out.println("111111");

        for (int i = 0; i < 2; i++) {
            Thread thread = new Thread(() -> {
                ThreadUtil.sleep(40);
                System.out.println(Thread.currentThread().getName());
                for (int j = 0; j < 2; j++) {
                    Thread threadi = new Thread(() -> {
                        ThreadUtil.sleep(40);
                        System.out.println(Thread.currentThread().getName());
                    });
                    threadi.start();
                    try {
                        threadi.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.start();
            thread.join();
        }

        System.out.println("222222");

        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(() -> {
                ThreadUtil.sleep(40);
                System.out.println(Thread.currentThread().getName());
            });
            thread.start();
            thread.join();
        }
        System.out.println("3333333333333");

    }
}
