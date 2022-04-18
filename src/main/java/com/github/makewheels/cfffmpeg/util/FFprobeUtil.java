package com.github.makewheels.cfffmpeg.util;

import cn.hutool.core.util.RuntimeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class FFprobeUtil {
    public static JSONObject getMeta(File file) {
        String cmd = PathUtil.getFFprobe() + " -show_streams -show_format -print_format json -v quiet \""
                + file.getAbsolutePath() + "\"";
        log.info(cmd);
        String json = RuntimeUtil.execForStr(cmd);
        log.info(json);
        return JSON.parseObject(json);
    }

    public static JSONObject getMeta(String url) {
        String cmd = PathUtil.getFFprobe() + " -show_streams -show_format -print_format json -v quiet " + url;
        log.info(cmd);
        String json = RuntimeUtil.execForStr(cmd);
        log.info(json);
        return JSON.parseObject(json);
    }

    public static JSONObject getAudioSteam(JSONObject meta) {
        JSONArray streams = meta.getJSONArray("streams");
        for (int i = 0; i < streams.size(); i++) {
            JSONObject stream = streams.getJSONObject(i);
            if (stream.getString("codec_type").equals("audio")) {
                return stream;
            }
        }
        return null;
    }

    public static JSONObject getVideoSteam(JSONObject meta) {
        JSONArray streams = meta.getJSONArray("streams");
        for (int i = 0; i < streams.size(); i++) {
            JSONObject stream = streams.getJSONObject(i);
            if (stream.getString("codec_type").equals("video")) {
                return stream;
            }
        }
        return null;
    }
}
