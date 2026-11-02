package com.sky.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * OSS连接健康检查工具
 * 启动时检查OSS连接状态
 */
@Component
@Slf4j
public class OssHealthChecker {

    @Autowired(required = false)
    private AliOssUtil aliOssUtil;

    @PostConstruct
    public void checkOssConnection() {
        if (aliOssUtil != null) {
            log.info("开始检查OSS连接状态...");
            
            try {
                // 检查基本配置
                String endpoint = aliOssUtil.getEndpoint();
                String accessKeyId = aliOssUtil.getAccessKeyId();
                String bucketName = aliOssUtil.getBucketName();
                
                log.info("OSS配置信息:");
                log.info("- Endpoint: {}", endpoint);
                log.info("- AccessKeyId: {}...", accessKeyId != null ? accessKeyId.substring(0, Math.min(5, accessKeyId.length())) : "null");
                log.info("- BucketName: {}", bucketName);
                
                // 验证Endpoint格式
                if (endpoint != null && endpoint.startsWith("http")) {
                    log.warn("警告: Endpoint格式可能不正确，不应包含'http://'前缀");
                }
                
                // 验证必要参数
                if (endpoint == null || endpoint.trim().isEmpty()) {
                    log.error("错误: OSS Endpoint未配置");
                    return;
                }
                if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
                    log.error("错误: OSS AccessKeyId未配置");
                    return;
                }
                if (bucketName == null || bucketName.trim().isEmpty()) {
                    log.error("错误: OSS BucketName未配置");
                    return;
                }
                
                log.info("OSS基础配置检查通过");
                
            } catch (Exception e) {
                log.error("OSS连接检查失败: {}", e.getMessage());
                log.error("建议检查网络连接和OSS配置");
            }
        } else {
            log.warn("未找到AliOssUtil Bean，跳过OSS连接检查");
        }
    }
}