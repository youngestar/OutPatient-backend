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

# 先安装所有模块到本地仓库，再打包data-analysis模块
RUN mvn clean install -DskipTests && \
    mvn clean package -pl OutPatient-backend-data-analysis -DskipTests

# 运行时镜像
FROM amazoncorretto:17-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建应用目录和用户（保留为 root 以便设置文件权限）
RUN addgroup -S appuser && adduser -S appuser -G appuser

# 创建数据目录
RUN mkdir -p /home/appuser/data/reports /home/appuser/data/models && \
    chown -R appuser:appuser /home/appuser && \
    chmod -R 755 /home/appuser

WORKDIR /app
# 从构建阶段复制jar包
# 复制打包后的可执行文件
COPY --from=builder /app/OutPatient-backend-data-analysis/target/OutPatient-backend-data-analysis-1.0-SNAPSHOT.jar /app/app.jar
# 复制等待脚本（来自模块目录），并确保行尾为 LF、赋可执行权限、调整所有者
COPY wait-for-services.sh /app/wait-for-services.sh
RUN sed -i 's/\r$//' /app/wait-for-services.sh || true && \
    chmod +x /app/wait-for-services.sh && \
    chown appuser:appuser /app/wait-for-services.sh

# 切换到非特权用户运行
USER appuser

# 暴露端口
EXPOSE 8204

# 使用等待脚本启动，确保 Redis/MySQL/Nacos 就绪后再启动应用
CMD ["sh","/app/wait-for-services.sh","java","-jar","app.jar"]
