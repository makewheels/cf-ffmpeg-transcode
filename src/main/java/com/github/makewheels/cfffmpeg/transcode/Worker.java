package com.github.makewheels.cfffmpeg.transcode;

import cn.hutool.core.util.RuntimeUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.CompletableFuture;

public class Worker {

    public String run(JSONObject body) {
        String cmd = body.getString("cmd");
        System.out.println(cmd);
        return CompletableFuture.supplyAsync(() -> RuntimeUtil.execForStr(cmd)).join();
    }

}
