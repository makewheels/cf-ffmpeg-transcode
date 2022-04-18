package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.github.makewheels.cfffmpeg.transcode.master.Master;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AliyunTranscodeMaster implements HttpRequestHandler {
    private Master master = new Master();

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
        JSONObject body = JSON.parseObject(IoUtil.readUtf8(request.getInputStream()));

        IoUtil.writeUtf8(response.getOutputStream(), true, "我是master");
    }

}
