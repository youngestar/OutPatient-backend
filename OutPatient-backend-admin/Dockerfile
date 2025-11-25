# 多阶段构建
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

# 先安装所有模块到本地仓库，再打包admin模块
RUN mvn clean install -DskipTests && \
    mvn clean package -pl OutPatient-backend-admin -DskipTests

# 运行时镜像
FROM amazoncorretto:17-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建应用目录和用户
RUN addgroup -S appuser && adduser -S appuser -G appuser

# 安装netcat用于服务健康检查
RUN apk add --no-cache netcat-openbsd

USER appuser

WORKDIR /app
# 从构建阶段复制jar包

COPY --from=builder /app/OutPatient-backend-admin/target/OutPatient-backend-admin-1.0-SNAPSHOT.jar app.jar

# 复制等待脚本并修复行结束符(BOM/CRLF)以避免容器内解析错误
COPY --chown=appuser:appuser wait-for-services.sh /app/wait-for-services.sh
RUN sed -i 's/\r$//' /app/wait-for-services.sh || true
RUN chmod +x /app/wait-for-services.sh

# 暴露端口
EXPOSE 8201

# 启动命令 - 先等待依赖服务，再启动应用
CMD ["sh", "/app/wait-for-services.sh", "java", "-jar", "app.jar"]
