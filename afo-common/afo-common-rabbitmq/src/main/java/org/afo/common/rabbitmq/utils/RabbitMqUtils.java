package org.afo.common.rabbitmq.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.afo.common.core.utils.SpringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ 工具类
 *
 * @author finops
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RabbitMqUtils {

    private static final Boolean RABBITMQ_ENABLE = SpringUtils.getProperty("rabbitmq.enabled", Boolean.class, false);

    /**
     * 发送消息到交换机
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     */
    public static void convertAndSend(String exchange, String routingKey, Object message) {
        if (!isEnable()) {
            return;
        }
        SpringUtils.getBean(RabbitTemplate.class).convertAndSend(exchange, routingKey, message);
    }

    /**
     * 发送消息到默认交换机
     *
     * @param routingKey 路由键，通常为队列名
     * @param message    消息
     */
    public static void convertAndSend(String routingKey, Object message) {
        if (!isEnable()) {
            return;
        }
        SpringUtils.getBean(RabbitTemplate.class).convertAndSend(routingKey, message);
    }

    /**
     * 是否开启 RabbitMQ
     */
    public static Boolean isEnable() {
        return RABBITMQ_ENABLE;
    }
}
