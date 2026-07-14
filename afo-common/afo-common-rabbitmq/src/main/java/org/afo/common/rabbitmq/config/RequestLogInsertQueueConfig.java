package org.afo.common.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "rabbitmq.enabled", havingValue = "true")
public class RequestLogInsertQueueConfig {

    public static final String QUEUE_NAME = "yy.llm.request-log.insert";
    public static final String EXCHANGE_NAME = "yy.topic";
    public static final String ROUTING_KEY = "yy.llm.request-log.insert";

    @Bean
    public DirectExchange requestLogInsertExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue requestLogInsertQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding requestLogInsertBinding(Queue requestLogInsertQueue, DirectExchange requestLogInsertExchange) {
        return BindingBuilder.bind(requestLogInsertQueue)
            .to(requestLogInsertExchange)
            .with(ROUTING_KEY);
    }
}
