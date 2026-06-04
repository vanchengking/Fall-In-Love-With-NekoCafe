# 多阶段构建：本机仅有 JDK8 + 无 Maven，统一在容器内用 JDK21 + Maven 构建。
# build 阶段会执行 mvn package（含单元测试），因此镜像构建成功即代表测试通过。
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY settings.xml ./settings.xml
# 先解析依赖以利用 Docker 层缓存，加快后续迭代构建
COPY pom.xml ./pom.xml
RUN mvn -B -s settings.xml -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -s settings.xml clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=build /app/target/neko-server.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
