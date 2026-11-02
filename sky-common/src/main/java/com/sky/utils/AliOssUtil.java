package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.ClientBuilderConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes 文件字节数组
     * @param objectName 对象名称
     * @return 文件访问路径
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient配置
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        // 连接超时时间，5秒
        conf.setConnectionTimeout(5000);
        // Socket超时时间，10秒
        conf.setSocketTimeout(10000);
        // 最大并发连接数
        conf.setMaxConnections(100);
        // 设置重试次数
        conf.setMaxErrorRetry(3);

        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);

        try {
            // 创建PutObject请求
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            log.error("OSS服务异常:");
            log.error("错误信息: {}", oe.getErrorMessage());
            log.error("错误代码: {}", oe.getErrorCode());
            log.error("请求ID: {}", oe.getRequestId());
            log.error("主机ID: {}", oe.getHostId());
            throw new RuntimeException("文件上传失败：" + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("OSS客户端异常，连接被拒绝: {}", ce.getMessage());
            log.error("可能原因：网络连接问题、防火墙阻止、Endpoint配置错误");
            log.error("请检查：1.网络连接 2.Endpoint格式 3.AccessKey权限 4.防火墙设置");
            throw new RuntimeException("文件上传失败：无法连接到OSS服务", ce);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 构建文件访问路径
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传成功，访问路径: {}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}