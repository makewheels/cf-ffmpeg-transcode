package com.github.makewheels.cfffmpeg;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RuntimeUtil;
import com.alibaba.fastjson.JSON;
import com.aliyun.fc_open20210406.Client;
import com.aliyun.fc_open20210406.models.Code;
import com.aliyun.fc_open20210406.models.UpdateFunctionHeaders;
import com.aliyun.fc_open20210406.models.UpdateFunctionRequest;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.github.makewheels.cfffmpeg.s3.S3Config;
import com.github.makewheels.cfffmpeg.s3.S3Service;

import java.io.File;

public class DeployUtil {
    public static Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        Config config = new Config().setAccessKeyId(accessKeyId).setAccessKeySecret(accessKeySecret);
        config.endpoint = "1618784280874658.cn-beijing.fc.aliyuncs.com";
        return new Client(config);
    }

    public static void main(String[] args) throws Exception {
        File project = new File("D:\\workSpace\\intellijidea\\cf-ffmpeg-transcode");
        File pom = new File(project, "pom.xml");
        File jar = new File(project, "target\\cf-ffmpeg-transcode-1.0-SNAPSHOT-jar-with-dependencies.jar");
        System.out.println(RuntimeUtil.execForStr("mvn.cmd clean package -f " + pom.getAbsolutePath()));

        String ak = Base64.decodeStr("TFRBSTV0U1RiaDlyc2pSOGtudUdyMldk");
        String sk = Base64.decodeStr("Q2VNR0JrWVFWR0ZFVzU0bVdXM1FuWHNJUFBmSE9a");

        S3Config config = new S3Config();
        String bucket = "cloud-function-bucket";
        config.setBucket(bucket);
        config.setAccessKey(ak);
        config.setSecretKey(sk);
        config.setEndpoint("oss-cn-beijing.aliyuncs.com");
        config.setRegion("cn-beijing");

        S3Service service = new S3Service();
        service.init(config);
        String object = "cf-ffmpeg-transcode/d.jar";
        System.out.println("开始上传对象存储 " + object);
        service.putObjectS3(object, jar);
        System.out.println("上传对象存储完成，开始部署：");

        Client client = createClient(ak, sk);
        UpdateFunctionRequest request = new UpdateFunctionRequest();
        request.setCode(new Code().setOssBucketName(bucket).setOssObjectName(object));

        System.out.println(JSON.toJSONString(client.updateFunctionWithOptions(
                "video-transcode", "transcode-master", request,
                new UpdateFunctionHeaders(), new RuntimeOptions())));

        System.out.println(JSON.toJSONString(client.updateFunctionWithOptions(
                "video-transcode", "transcode-worker", request,
                new UpdateFunctionHeaders(), new RuntimeOptions())));

        System.out.println(JSON.toJSONString(client.updateFunctionWithOptions(
                "video-transcode", "ffprobe", request,
                new UpdateFunctionHeaders(), new RuntimeOptions())));

        System.out.println(JSON.toJSONString(client.updateFunctionWithOptions(
                "video-transcode", "clean", request,
                new UpdateFunctionHeaders(), new RuntimeOptions())));

        service.deleteObject(object);
    }
}
