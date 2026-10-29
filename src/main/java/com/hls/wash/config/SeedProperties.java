package com.hls.wash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内置四个真实模型（e107/e221/e222/e302）的初始化开关。
 *
 * <p>初始化只插入常量表中缺失的模型，不会覆盖管理页面维护过的数据。</p>
 */
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
