# 多阶段构建 - 使用更稳定的镜像标签
FROM maven:3.9.6-amazoncorretto-17 AS builder

WORKDIR /app
COPY . .

# 设置阿里云镜像并构建
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?> \
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
              xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
              http://maven.apache.org/xsd/settings-1.0.0.xsd"> \
        <mirrors> \
            <mirror> \
                <id>aliyunmaven</id> \
                <mirrorOf>*</mirrorOf> \
                <name>阿里云公共仓库</name> \
                <url>https://maven.aliyun.com/repository/public</url> \
            </mirror> \
        </mirrors> \
    </settings>' > /root/.m2/settings.xml

# 先安装所有模块到本地仓库，再打包gateway模块
RUN mvn clean install -DskipTests && \
    mvn clean package -pl OutPatient-backend-gateway -DskipTests

# 运行时镜像
FROM amazoncorretto:17-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建应用目录和用户
RUN addgroup -S appuser && adduser -S appuser -G appuser

WORKDIR /app
# 从构建阶段复制jar包
COPY --from=builder /app/OutPatient-backend-gateway/target/OutPatient-backend-gateway-1.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8200

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]