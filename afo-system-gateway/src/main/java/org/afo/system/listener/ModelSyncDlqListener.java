package org.afo.system.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.rabbitmq.config.ModelSyncQueueConfig;
import org.afo.common.rabbitmq.message.ModelSyncMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rabbitmq.enabled", havingValue = "true")
public class ModelSyncDlqListener {

    @RabbitListener(queues = ModelSyncQueueConfig.DLQ_NAME)
    public void handleDlq(ModelSyncMessage msg) {
        log.error("[DLQ] Model sync message failed after retries: action={}, modelId={}, modelCode={}, tenantId={}",
            msg.getAction(), msg.getModelId(), msg.getModelCode(), msg.getTenantId());
    }
}
