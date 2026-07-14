package org.afo.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LiteLLM Proxy 配置属性
 * 
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "afo.gateway.litellm")
public class LiteLLMProperties {

    /** LiteLLM Proxy 地址 */
    private String baseUrl = "http://127.0.0.1:4000";
    
    /** 请求超时（毫秒），需 ≥ Netty responseTimeout(120s) 以兼容 SSE 长流 */
    private int timeout = 30000;
    
    /** 重试次数 */
    private int retryCount = 2;

}
