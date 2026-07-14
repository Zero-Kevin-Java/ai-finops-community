package org.afo.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分类器服务配置属性
 * 
 * L0阶段仅用于心跳检查，不参与路由决策
 * 
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "afo.gateway.classifier")
public class ClassifierProperties {

    /** 分类器服务地址 */
    private String baseUrl = "http://127.0.0.1:8000";
    
    /** 请求超时（毫秒） */
    private int timeout = 5000;
    
    /** 心跳检查间隔（毫秒） */
    private long heartbeatInterval = 30000;

}
