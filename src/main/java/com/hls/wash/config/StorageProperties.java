package com.hls.wash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RustFS（S3 兼容）对象存储配置。
 *
 * <p>凭证只允许来自环境变量或未提交到 Git 的本地 profile，
 * 不得写入源码、前端代码或 Android 代码。</p>
 */
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** S3 API endpoint，例如 http://10.32.22.200:9000（不是控制台端口 9001）。 */
    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
    private String bucket = "wash-model-images";
    private String region = "us-east-1";
    private boolean autoCreateBucket = true;
    /** 连接超时，秒。 */
    private int connectTimeoutSeconds = 10;
    /** 单次调用超时，秒。 */
    private int requestTimeoutSeconds = 30;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isAutoCreateBucket() {
        return autoCreateBucket;
    }

    public void setAutoCreateBucket(boolean autoCreateBucket) {
        this.autoCreateBucket = autoCreateBucket;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public boolean hasCredentials() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
