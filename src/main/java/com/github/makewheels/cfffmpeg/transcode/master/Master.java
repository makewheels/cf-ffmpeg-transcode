package com.github.makewheels.cfffmpeg.transcode.master;

import com.alibaba.fastjson.JSONObject;
import com.github.makewheels.cfffmpeg.util.PathUtil;

public class Master {
    public void run(JSONObject body) {
        PathUtil.initMissionFolder(body.getString("missionId"));
    }
}
