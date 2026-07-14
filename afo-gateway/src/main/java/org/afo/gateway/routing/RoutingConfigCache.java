package org.afo.gateway.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关侧路由配置缓存。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingConfigCache {

    private String tenantId;
    private boolean present;
    @Builder.Default
    private List<RoutingConfigRuleCache> rules = new ArrayList<>();

    public static RoutingConfigCache empty() {
        return RoutingConfigCache.builder().present(false).rules(new ArrayList<>()).build();
    }
}
