package com.github.makewheels.cfffmpeg.aliyun;

import cn.hutool.core.io.IoUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.github.makewheels.cfffmpeg.ffprobe.FFprobe;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.TimeZone;

public class AliyunFFprobe implements HttpRequestHandler {
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        JSONObject body = JSON.parseObject(IoUtil.readUtf8(request.getInputStream()));
        String result = new FFprobe().getMeta(body);
        response.setHeader(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue());
        IoUtil.writeUtf8(response.getOutputStream(), true, result);
    }

}
