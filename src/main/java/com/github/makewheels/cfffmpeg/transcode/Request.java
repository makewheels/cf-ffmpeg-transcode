package com.github.makewheels.cfffmpeg.transcode;

import lombok.Data;

@Data
public class Request {
    private String bucket;
    private String inputKey;
    private String outputFolder;
    private String m3u8Prefix;

    private String resolution;
    private String quality;

}
