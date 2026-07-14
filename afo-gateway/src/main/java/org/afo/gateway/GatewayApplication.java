package org.afo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI-FinOps 网关启动类
 * 
 * L0稳态版：透明代理 + 分类器就绪
 * 
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(basePackages = {"org.afo.gateway"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("  AI-FinOps Gateway started successfully");
        System.out.println("  Port: 8081");
        System.out.println("========================================");
    }

}
