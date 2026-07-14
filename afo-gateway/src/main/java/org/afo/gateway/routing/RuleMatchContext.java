package org.afo.gateway.routing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 路由规则匹配上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleMatchContext {

    private String tenantId;
    private String requestId;
    private String apiKeyId;
    private String teamTag;
    private String department;
    private String userId;
    private String appId;
    private String path;
    private String sourceModel;
    private String modelType;
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    private JsonNode request;
}
