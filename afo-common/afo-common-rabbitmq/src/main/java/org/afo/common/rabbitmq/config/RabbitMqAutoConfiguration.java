package org.afo.common.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * RabbitMQ 自动装配
 *
 * @author finops
 */
@EnableRabbit
@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass({RabbitTemplate.class, ConnectionFactory.class})
@ConditionalOnProperty(value = "rabbitmq.enabled", havingValue = "true")
@EnableConfigurationProperties(RabbitMqProperties.class)
@Import({
    SimpleTaskRouteQueueConfig.class,
    ModelSyncQueueConfig.class,
    RequestLogQueueConfig.class
})
public class RabbitMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Jackson2JsonMessageConverter rabbitMqMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplateCustomizer rabbitMqTemplateCustomizer(Jackson2JsonMessageConverter rabbitMqMessageConverter) {
        return rabbitTemplate -> rabbitTemplate.setMessageConverter(rabbitMqMessageConverter);
    }

}
