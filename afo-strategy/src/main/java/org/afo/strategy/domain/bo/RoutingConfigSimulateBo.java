package org.afo.strategy.domain.bo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 路由配置模拟请求。
 */
@Data
public class RoutingConfigSimulateBo {

    private String tenantId;
    private String apiKeyId;
    private List<String> apiKeyIds;
    private String teamTag;
    private String department;
    private List<String> departments;
    private String userId;
    private List<String> userIds;
    private String appId;
    private List<String> appIds;
    private String path;
    private String sourceModel;
    private List<String> sourceModels;
    private String modelType;
    private String prompt;
    private String input;
    private String messagesText;
    private List<String> tools;
    private Map<String, String> headers;
}
