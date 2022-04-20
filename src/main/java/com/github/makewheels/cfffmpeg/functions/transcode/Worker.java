package com.github.makewheels.cfffmpeg.functions.transcode;

import cn.hutool.core.util.RuntimeUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Worker {

    public String run(JSONObject body) {
        String cmd = body.getString("cmd");
        log.info(cmd);
        AtomicReference<String> result = new AtomicReference<>();
        Thread thread = new Thread(() -> result.set(RuntimeUtil.execForStr(cmd)));
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info(result.get());
        return result.get();
    }

}
