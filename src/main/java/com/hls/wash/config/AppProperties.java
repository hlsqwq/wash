package com.hls.wash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用级配置。
 *
 * <p>publicBaseUrl 为空时，图片地址由当前 HTTP 请求的 Host 动态生成；
 * 生产环境建议通过 APP_PUBLIC_BASE_URL 显式指定，避免反向代理下生成内网地址。</p>
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String publicBaseUrl = "";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
