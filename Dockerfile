FROM registry.cn-beijing.aliyuncs.com/b4/openjdk:11
EXPOSE 9000
ADD "target/cf-ffmpeg-transcode-1.0-SNAPSHOT-jar-with-dependencies.jar" "/app.jar"
CMD ["java","-jar","/app.jar"]