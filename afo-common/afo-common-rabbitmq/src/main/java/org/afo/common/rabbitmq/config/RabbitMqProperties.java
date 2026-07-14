package org.afo.common.rabbitmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ 配置属性
 *
 * @author finops
 */
@Data
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {

    /**
     * 是否开启 RabbitMQ 公共配置
     */
    private Boolean enabled = false;

    /**
     * 示例配置
     */
    private Demo demo = new Demo();

    @Data
    public static class Demo {

        /**
         * 是否开启示例队列、交换机、绑定和监听器
         */
        private Boolean enabled = false;

        /**
         * 示例交换机名称
         */
        private String exchangeName = "finops.demo.exchange";

        /**
         * 示例队列名称
         */
        private String queueName = "finops.demo.queue";

        /**
         * 示例路由键
         */
        private String routingKey = "finops.demo.routing-key";
    }
}
