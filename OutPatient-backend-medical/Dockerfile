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

# 先安装所有模块到本地仓库，再打包medical模块
RUN mvn clean install -DskipTests && \
    mvn clean package -pl OutPatient-backend-medical -DskipTests

# 运行时镜像
FROM amazoncorretto:17-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建应用目录和用户
RUN addgroup -S appuser && adduser -S appuser -G appuser

# 创建模板目录
RUN mkdir -p /home/appuser/data/templates && \
    chown -R appuser:appuser /home/appuser && \
    chmod -R 755 /home/appuser
# 把打包好的 jar 从构建阶段拷贝到运行阶段
COPY --from=builder /app/OutPatient-backend-medical/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar || true

# 在镜像内以 root 生成等待脚本（避免 BOM/CRLF 导致的解析问题）
RUN cat > /app/wait-for-services.sh <<'EOF'
#!/bin/sh

# 等待 Redis 服务
echo "等待Redis服务启动..."
while ! nc -z redis 6379; do
    sleep 1
done
echo "Redis服务已启动"

# 等待 MySQL 服务
echo "等待MySQL服务启动..."
while ! nc -z mysql 3306; do
    sleep 1
done
echo "MySQL服务已启动"

# 等待 Nacos 服务
echo "等待Nacos服务启动..."
while ! nc -z nacos 8848; do
    sleep 1
done
echo "Nacos服务已启动"

# 执行应用启动命令
exec "$@"
EOF

RUN tr -d '\r' < /app/wait-for-services.sh > /app/wait-for-services.sh.tmp || true && \
    mv /app/wait-for-services.sh.tmp /app/wait-for-services.sh || true && \
    chown appuser:appuser /app/wait-for-services.sh && chmod +x /app/wait-for-services.sh || true

USER appuser

WORKDIR /app

# 暴露端口
EXPOSE 8205

# 启动命令（先等待依赖再启动应用）
CMD ["sh", "/app/wait-for-services.sh", "java", "-jar", "app.jar"]
