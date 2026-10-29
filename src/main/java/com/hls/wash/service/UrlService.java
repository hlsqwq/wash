package com.hls.wash.service;

import com.hls.wash.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 生成对 Android 和浏览器均可访问的图片地址。
 *
 * <p>优先使用 APP_PUBLIC_BASE_URL；未配置时使用当前请求的上下文地址。
 * 绝不把 RustFS 控制台地址或存储凭证下发给客户端。</p>
 */
@Service
public class UrlService {

    private final AppProperties appProperties;

    public UrlService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String imageUrl(String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            return null;
        }
        return baseUrl() + "/api/v1/files/images/" + mediaId;
    }

    public String baseUrl() {
        String configured = appProperties.getPublicBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return stripTrailingSlash(configured.trim());
        }
        try {
            return stripTrailingSlash(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
        } catch (IllegalStateException noRequest) {
            // 后台任务（例如种子数据）没有请求上下文时使用本地默认值。
            return "http://localhost:8080";
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
