package com.github.makewheels.cfffmpeg.s3;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.model.PutObjectResult;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class S3Service {
    private String bucket;

    private OSS client;

    /**
     * 给线上云函数调用初始化用
     */
    public void init(String bucket, String endpoint) {
        if (client != null) return;
        this.bucket = bucket;
        String accessKeyId = System.getenv("s3_accessKeyId");
        String secretKey = System.getenv("s3_secretKey");

        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setProtocol(Protocol.HTTP);
        client = new OSSClientBuilder().build(endpoint, accessKeyId, secretKey, configuration);
    }

    /**
     * 给我本地部署云函数用
     */
    public void init(S3Config config) {
        if (client != null) return;
        this.bucket = config.getBucket();

        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setProtocol(Protocol.HTTP);
        client = new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKey(),
                config.getSecretKey(), configuration);
    }

    /**
     * 上传File
     */
    public void putObjectS3(String key, File file) {
        client.putObject(bucket, key, file);
    }

    /**
     * 删除object
     */
    public void deleteObject(String key) {
        client.deleteObject(bucket, key);
    }

    /**
     * 上传File
     */
    public PutObjectResult putObject(String key, File file) {
        return client.putObject(bucket, key, file);
    }

    /**
     * 预签名
     */
    public String signGetUrl(String key) {
        return client.generatePresignedUrl(
                bucket, key, Date.from(Instant.now().plus(Duration.ofHours(1)))).toString();
    }

}
