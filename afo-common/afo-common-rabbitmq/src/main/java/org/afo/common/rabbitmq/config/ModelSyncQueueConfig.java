package org.afo.common.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "rabbitmq.enabled", havingValue = "true")
public class ModelSyncQueueConfig {

    public static final String EXCHANGE_NAME = "yy.model.sync";
    public static final String QUEUE_NAME = "yy.model.sync";
    public static final String ROUTING_KEY = "yy.model.sync";
    public static final String DLX_NAME = "yy.model.sync.dlx";
    public static final String DLQ_NAME = "yy.model.sync.dlq";

    @Bean
    public DirectExchange modelSyncExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue modelSyncQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
            .deadLetterExchange(DLX_NAME)
            .deadLetterRoutingKey(ROUTING_KEY)
            .build();
    }

    @Bean
    public Binding modelSyncBinding(Queue modelSyncQueue, DirectExchange modelSyncExchange) {
        return BindingBuilder.bind(modelSyncQueue)
            .to(modelSyncExchange)
            .with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange modelSyncDlx() {
        return ExchangeBuilder.directExchange(DLX_NAME).durable(true).build();
    }

    @Bean
    public Queue modelSyncDlq() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding modelSyncDlqBinding(Queue modelSyncDlq, DirectExchange modelSyncDlx) {
        return BindingBuilder.bind(modelSyncDlq)
            .to(modelSyncDlx)
            .with(ROUTING_KEY);
    }
}
