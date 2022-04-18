package com.github.makewheels.cfffmpeg.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3Config {
    private String bucket;
    private String region;
    private String endpoint;

    private String accessKey;
    private String secretKey;

}
