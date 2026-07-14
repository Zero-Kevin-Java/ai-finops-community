package org.afo.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Gateway WebClient 配置
 * 
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@Configuration
public class GatewayWebClientConfig {

    @Value("${afo.gateway.admin.base-url:http://127.0.0.1:8080}")
    private String adminBaseUrl;

    @Value("${afo.gateway.litellm.base-url:http://127.0.0.1:4000}")
    private String litellmBaseUrl;

    @Bean
    public WebClient adminWebClient() {
        return WebClient.builder()
            .baseUrl(adminBaseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Bean
    public WebClient litellmWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("llm-router-pool")
            .maxConnections(500)
            .pendingAcquireMaxCount(1000)
            .pendingAcquireTimeout(Duration.ofSeconds(10))
            .maxIdleTime(Duration.ofSeconds(60))
            .maxLifeTime(Duration.ofMinutes(5))
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(120))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl(litellmBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
