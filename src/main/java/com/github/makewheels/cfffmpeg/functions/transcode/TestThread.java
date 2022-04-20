package com.github.makewheels.cfffmpeg.functions.transcode;

import cn.hutool.core.thread.ThreadUtil;

public class TestThread {
    public static void main(String[] args) throws InterruptedException {
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
