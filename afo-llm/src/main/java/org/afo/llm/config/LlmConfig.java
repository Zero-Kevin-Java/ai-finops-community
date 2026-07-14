package org.afo.llm.config;

import org.afo.common.core.utils.crypto.AesEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * LLM 模块配置。
 *
 * @author afo
 */
@Configuration
public class LlmConfig {

    @Value("${afo.crypto.model-key:YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=}")
    private String modelKey;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AesEncryptor aesEncryptor() {
        return new AesEncryptor(modelKey);
    }

    @Value("${rabbitmq.enabled:false}")
    private boolean rabbitmqEnabled;

    @Bean
    public boolean rabbitmqEnabled() {
        return rabbitmqEnabled;
    }
}
