package com.github.makewheels.cfffmpeg.ffprobe;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.makewheels.cfffmpeg.s3.S3Service;
import com.github.makewheels.cfffmpeg.util.FFmpegUtil;

public class FFprobe {
    private final S3Service s3Service = new S3Service();

    public String getMeta(String json) {
        JSONObject body = JSON.parseObject(json);
        s3Service.init(body.getString("bucket"), body.getString("region"), body.getString("endpoint"));
        String inputKey = body.getString("inputKey");
        String url = s3Service.getSignedGetUrl(inputKey);
        return FFmpegUtil.getMeta(url);
    }
}
