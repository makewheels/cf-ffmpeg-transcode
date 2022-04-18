package com.github.makewheels.cfffmpeg.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class S3Service {
    private String endpoint;
    private String region;

    private String accessKey;
    private String secretKey;

    private String bucketName;

    private AmazonS3 amazonS3;

    public void init(S3Config config) {
        if (amazonS3 != null) {
            return;
        }
        AWSCredentials credentials = new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey());
        AwsClientBuilder.EndpointConfiguration configuration = new AwsClientBuilder.EndpointConfiguration(
                config.getEndpoint(), config.getRegion());
        amazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(
                credentials)).withEndpointConfiguration(configuration).build();
        bucketName = config.getBucketName();
        region = config.getRegion();
    }

    private AmazonS3 getClient() {
        return amazonS3;
    }

    /**
     * 获取object
     *
     * @param key
     * @return
     */
    public S3Object getObject(String key) {
        return getClient().getObject(bucketName, key);
    }

    /**
     * 上传File
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        return getClient().putObject(bucketName, key, file);
    }

    /**
     * 删除object
     *
     * @param key
     */
    public void deleteObject(String key) {
        getClient().deleteObject(bucketName, key);
    }

    /**
     * 批量删除objects
     *
     * @param keys
     * @return
     */
    public DeleteObjectsResult deleteObjects(List<String> keys) {
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        List<DeleteObjectsRequest.KeyVersion> keyVersions = new ArrayList<>(keys.size());
        for (String key : keys) {
            DeleteObjectsRequest.KeyVersion keyVersion = new DeleteObjectsRequest.KeyVersion(key);
            keyVersions.add(keyVersion);
        }
        request.setKeys(keyVersions);
        return getClient().deleteObjects(request);
    }

    /**
     * 预签名，可指定HttpMethod
     *
     * @param key
     * @param duration
     * @param httpMethod
     * @return
     */
    public String generatePresignedUrl(String key, Duration duration, HttpMethod httpMethod) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key)
                .withMethod(httpMethod).withExpiration(Date.from(Instant.now().plus(duration)));
        return getClient().generatePresignedUrl(request).toString();
    }

    /**
     * 从对象存储下载文件到本地
     */
    public ObjectMetadata download(String key, File file) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        return getClient().getObject(getObjectRequest, file);
    }

}
