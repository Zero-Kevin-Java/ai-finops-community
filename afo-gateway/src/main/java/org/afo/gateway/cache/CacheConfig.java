package org.afo.gateway.cache;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CacheConfig {

    private Long configId;
    private String enabled;
    private String matchMode;
    private BigDecimal similarityThreshold;
    private Integer ttlSeconds;
    private Integer maxEntries;

    public boolean isEnabled() {
        return "1".equals(enabled);
    }
}
