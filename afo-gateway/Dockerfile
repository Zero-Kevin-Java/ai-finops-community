# 贝尔实验室 Spring 官方推荐镜像
FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds

LABEL maintainer="AI-FinOps Team"

RUN microdnf install -y curl && microdnf clean all

RUN mkdir -p /afo/gateway/logs \
    /afo/gateway/temp

WORKDIR /afo/gateway

ENV SERVER_PORT=8081 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE 8081

ADD ./target/afo-gateway.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -Dserver.port=${SERVER_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
