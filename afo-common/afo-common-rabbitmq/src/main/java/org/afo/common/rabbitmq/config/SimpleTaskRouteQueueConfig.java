package org.afo.common.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 简单任务路由变更队列配置
 *
 * Admin 写入 DB → afterCommit 发布 MQ → Gateway Consumer 回写 Redis Hash
 *
 * @author AI-FinOps Team
 * @since 2026-05-12
 */
@Configuration
@ConditionalOnProperty(value = "rabbitmq.enabled", havingValue = "true")
public class SimpleTaskRouteQueueConfig {

    public static final String EXCHANGE_NAME = "yy.simple.route";
    public static final String QUEUE_NAME = "yy.simple.route";
    public static final String ROUTING_KEY = "yy.simple.route";
    public static final String DLX_NAME = "yy.simple.route.dlx";
    public static final String DLQ_NAME = "yy.simple.route.dlq";

    @Bean
    public DirectExchange simpleTaskRouteExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue simpleTaskRouteQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
            .deadLetterExchange(DLX_NAME)
            .deadLetterRoutingKey(ROUTING_KEY)
            .build();
    }

    @Bean
    public Binding simpleTaskRouteBinding(
            Queue simpleTaskRouteQueue,
            DirectExchange simpleTaskRouteExchange) {
        return BindingBuilder.bind(simpleTaskRouteQueue)
            .to(simpleTaskRouteExchange)
            .with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange simpleTaskRouteDlx() {
        return ExchangeBuilder.directExchange(DLX_NAME).durable(true).build();
    }

    @Bean
    public Queue simpleTaskRouteDlq() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding simpleTaskRouteDlqBinding(
            Queue simpleTaskRouteDlq,
            DirectExchange simpleTaskRouteDlx) {
        return BindingBuilder.bind(simpleTaskRouteDlq)
            .to(simpleTaskRouteDlx)
            .with(ROUTING_KEY);
    }
}
