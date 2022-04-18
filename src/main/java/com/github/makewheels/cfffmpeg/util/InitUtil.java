package com.github.makewheels.cfffmpeg.util;

import com.alibaba.fastjson.JSONObject;

public class InitUtil {
    public void init(JSONObject body) {
        PathUtil.initMissionFolder(body.getString("missionId"));
    }
}
