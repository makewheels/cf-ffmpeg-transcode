package com.github.makewheels.cfffmpeg.util;

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
    private boolean useInternal;

    private String endpoint;
    private String internalEndpoint;

    private String region;

    private String accessKey;
    private String secretKey;

    private String bucketName;

    private String domain;
    private String cdnDomain;
    private String internalDomain;

    private AmazonS3 amazonS3;

    /**
     * 初始化
     */
    public void init() {
        if (amazonS3 != null) {
            return;
        }
        String initEndPoint;
        if (useInternal) {
            initEndPoint = internalEndpoint;
        } else {
            initEndPoint = endpoint;
        }
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AwsClientBuilder.EndpointConfiguration configuration = new AwsClientBuilder.EndpointConfiguration(initEndPoint, region);
        amazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withEndpointConfiguration(configuration).build();
    }

    public void init(S3Config config) {
        if (amazonS3 != null) {
            return;
        }
        AWSCredentials credentials = new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey());
        AwsClientBuilder.EndpointConfiguration configuration = new AwsClientBuilder.EndpointConfiguration(config.getEndpoint(), config.getRegion());
        amazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withEndpointConfiguration(configuration).build();
        bucketName = config.getBucketName();
        region = config.getRegion();
    }

    private AmazonS3 getClient() {
        if (amazonS3 == null) {
            init();
        }
        return amazonS3;
    }

    public String getBucketName() {
        return bucketName;
    }

    /**
     * object是否存在
     *
     * @param key
     * @return
     */
    public boolean doesObjectExist(String key) {
        return getClient().doesObjectExist(bucketName, key);
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
     * 根据prefix查询objects
     *
     * @param prefix
     * @return
     */
    public ObjectListing listObjects(String prefix) {
        return getClient().listObjects(bucketName, prefix);
    }

    /**
     * 查询objects
     *
     * @return
     */
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) {
        return getClient().listObjects(listObjectsRequest.withBucketName(bucketName));
    }

    /**
     * 查询objects
     *
     * @return
     */
    public ListObjectsV2Result listObjectsV2(ListObjectsV2Request listObjectsV2Request) {
        return getClient().listObjectsV2(listObjectsV2Request.withBucketName(bucketName));
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
     * 上传File
     *
     * @param putObjectRequest
     * @return
     */
    public PutObjectResult putObject(PutObjectRequest putObjectRequest) {
        putObjectRequest.withBucketName(bucketName);
        return getClient().putObject(putObjectRequest);
    }

    /**
     * 上传String content
     *
     * @param key
     * @param content
     * @return
     */
    public PutObjectResult putObject(String key, String content) {
        return getClient().putObject(bucketName, key, content);
    }

    /**
     * copyObject
     *
     * @param from
     * @param to
     * @return
     */
    public CopyObjectResult copyObject(String from, String to) {
        return getClient().copyObject(bucketName, from, bucketName, to);
    }

    /**
     * copyObject
     *
     * @param copyObjectRequest
     * @return
     */
    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest) {
        return getClient().copyObject(copyObjectRequest);
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
     * 获取url
     *
     * @param key
     * @return
     */
    public String getUrl(String key) {
        String url = getClient().getUrl(bucketName, key).toString();
        return url.replaceFirst(internalDomain, domain);
    }

    /**
     * 获取cdn url
     *
     * @param key
     * @return
     */
    public String getCdnUrl(String key) {
        String url = getClient().getUrl(bucketName, key).toString();
        url = url.replaceFirst(domain, cdnDomain);
        url = url.replaceFirst(internalDomain, cdnDomain);
        return url;
    }

    /**
     * 获取internal url
     *
     * @param key
     * @return
     */
    public String getInternalUrl(String key) {
        String url = getClient().getUrl(bucketName, key).toString();
        return url.replaceFirst(domain, internalDomain);
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
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key).withMethod(httpMethod).withExpiration(Date.from(Instant.now().plus(duration)));
        return getClient().generatePresignedUrl(request).toString();
    }

}
