#!/bin/sh

# 等待Redis服务
echo "等待Redis服务启动..."
while ! nc -z redis 6379; do
  sleep 1
done
echo "Redis服务已启动"

# 等待MySQL服务
echo "等待MySQL服务启动..."
while ! nc -z mysql 3306; do
  sleep 1
done
echo "MySQL服务已启动"

# 等待Nacos服务
echo "等待Nacos服务启动..."
while ! nc -z nacos 8848; do
  sleep 1
done
echo "Nacos服务已启动"

# 执行应用启动命令
exec "$@"