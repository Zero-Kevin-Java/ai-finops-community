package org.afo.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关缓存配置属性。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Data
@Component
@ConfigurationProperties(prefix = "afo.gateway.cache")
public class GatewayCacheConfig {

    /** API Key 缓存 TTL（秒）。 */
    private int apiKeyTtl = 300;

    /** 模型准入策略缓存 TTL（秒）。 */
    private int modelAccessTtl = 600;

    /** 缓存配置 TTL（秒）。 */
    private int cacheConfigTtl = 600;

    /** Redis Pub/Sub 刷新 Topic。 */
    private String refreshTopic = "gateway:cache:refresh";
}
