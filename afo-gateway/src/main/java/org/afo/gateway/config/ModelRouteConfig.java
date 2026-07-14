package org.afo.gateway.config;

import lombok.Data;

/**
 * 模型路由配置（从 Redis 读取）。
 *
 * @author afo
 */
@Data
public class ModelRouteConfig {
    private String litellmModel;
    private String apiKey;
    private String apiBase;
    private String protocol;
    private String apiVersion;
}
