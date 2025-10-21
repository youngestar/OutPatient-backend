#!/bin/bash

echo "开始构建医院管理系统..."

# 停止现有服务
docker-compose down

echo "在宿主机上构建所有Maven模块..."
mvn clean install -DskipTests

echo "构建Docker镜像..."
docker-compose build

echo "启动所有服务..."
docker-compose up -d

echo "服务启动完成！"
echo ""
echo "访问地址："
echo "网关服务: http://localhost:8200"
echo "Nacos 控制台: http://localhost:8848/nacos"
echo "MinIO 控制台: http://localhost:9001"
echo ""
echo "查看服务状态: docker-compose ps"
echo "查看日志: docker-compose logs -f [服务名]"