package com.github.makewheels.cfffmpeg.ffprobe;

import com.alibaba.fastjson.JSONObject;
import com.github.makewheels.cfffmpeg.s3.S3Service;
import com.github.makewheels.cfffmpeg.util.FFprobeUtil;

public class FFprobe {
    private final S3Service s3Service = new S3Service();

    public String getMeta(JSONObject body) {
        s3Service.init(body.getString("bucket"), body.getString("region"), body.getString("endpoint"));
        String inputKey = body.getString("inputKey");
        String url = s3Service.signGetUrl(inputKey);
        JSONObject meta = FFprobeUtil.getMeta(url);
        return meta.toJSONString();
    }
}
