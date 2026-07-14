package org.afo.common.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 路由决策日志队列配置
 * 
 * 定义 yy.request.log 队列，用于网关异步投递路由决策日志到 afo-logs 消费端
 * 
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@Configuration
@ConditionalOnProperty(value = "rabbitmq.enabled", havingValue = "true")
public class RequestLogQueueConfig {

    public static final String QUEUE_NAME = "yy.request.log";
    public static final String EXCHANGE_NAME = "yy.topic";
    public static final String ROUTING_KEY = "yy.request.log";

    @Bean
    public DirectExchange requestLogExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue requestLogQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding requestLogBinding(Queue requestLogQueue, DirectExchange requestLogExchange) {
        return BindingBuilder.bind(requestLogQueue)
            .to(requestLogExchange)
            .with(ROUTING_KEY);
    }
}
