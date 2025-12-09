package com.std.cuit.auth.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author: withlia
 * date: 2025/11/03
 * description: minio 配置
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 检查存储桶是否存在，不存在则创建
            createBucketIfNotExists(minioClient);

            return minioClient;
        } catch (Exception e) {
            log.error("初始化MinIO客户端失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化MinIO客户端失败", e);
        }
    }

    /**
     * 创建存储桶（如果不存在）
     */
    private void createBucketIfNotExists(MinioClient minioClient) {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("创建MinIO存储桶成功: {}", bucketName);

                // 可选：设置存储桶策略为公共读
                // setBucketPolicy(minioClient);
            } else {
                log.info("MinIO存储桶已存在: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("检查/创建存储桶失败: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO存储桶操作失败", e);
        }
    }
}