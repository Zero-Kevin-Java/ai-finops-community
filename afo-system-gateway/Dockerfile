# 贝尔实验室 Spring 官方推荐镜像
FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds

LABEL maintainer="AI-FinOps Community"

RUN microdnf install -y curl && microdnf clean all

RUN mkdir -p /afo/system-gateway/logs \
    /afo/system-gateway/temp

WORKDIR /afo/system-gateway

ENV SERVER_PORT=8080 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE 8080

ADD ./target/afo-system-gateway.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -Dserver.port=${SERVER_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
