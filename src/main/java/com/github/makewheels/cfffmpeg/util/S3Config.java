package com.github.makewheels.cfffmpeg.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3Config {
    private String endpoint;
    private String internalEndpoint;

    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    private String domain;
    private String cdnDomain;
    private String internalDomain;

}
