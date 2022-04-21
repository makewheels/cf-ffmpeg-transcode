FROM openjdk:11
EXPOSE 9000
ADD "target/cf-ffmpeg-transcode-1.0-SNAPSHOT-jar-with-dependencies.jar" "/app.jar"
CMD ["java","-jar","/app.jar"]