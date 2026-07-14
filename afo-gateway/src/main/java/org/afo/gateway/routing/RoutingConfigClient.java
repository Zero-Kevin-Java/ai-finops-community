package org.afo.gateway.routing;

/**
 * 路由配置加载客户端。
 */
@FunctionalInterface
public interface RoutingConfigClient {

    RoutingConfigCache loadActiveConfig(String tenantId);
}
