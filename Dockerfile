FROM registry-internal.cn-beijing.aliyuncs.com/b4/transcode-base:latest
EXPOSE 9000
ADD "target/cf-ffmpeg-transcode-1.0-SNAPSHOT.jar" "/app.jar"
CMD ["java","-jar","/app.jar"]