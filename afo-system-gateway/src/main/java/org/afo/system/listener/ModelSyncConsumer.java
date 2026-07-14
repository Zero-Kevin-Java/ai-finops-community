package org.afo.system.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.core.utils.crypto.AesEncryptor;
import org.afo.common.rabbitmq.config.ModelSyncQueueConfig;
import org.afo.common.rabbitmq.message.ModelSyncMessage;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rabbitmq.enabled", havingValue = "true")
public class ModelSyncConsumer {

    private final LlmModelCatalogMapper catalogMapper;
    private final StringRedisTemplate redisTemplate;
    private final AesEncryptor aesEncryptor;

    @RabbitListener(queues = ModelSyncQueueConfig.QUEUE_NAME)
    public void handle(ModelSyncMessage msg) {
        log.debug("[ModelSyncConsumer] Received: action={}, modelId={}, modelCode={}",
            msg.getAction(), msg.getModelId(), msg.getModelCode());

        switch (msg.getAction()) {
            case "CREATE":
            case "UPDATE":
                handleCreateOrUpdate(msg);
                break;
            case "DELETE":
            case "DISABLE":
                handleDelete(msg);
                break;
            default:
                log.warn("[ModelSyncConsumer] Unknown action: {}", msg.getAction());
        }
    }

    private void handleCreateOrUpdate(ModelSyncMessage msg) {
        LlmModelCatalog model = catalogMapper.selectById(msg.getModelId());
        if (model == null) {
            log.warn("[ModelSyncConsumer] Model not found in DB: modelId={}", msg.getModelId());
            return;
        }
        if (msg.getOldModelCode() != null && !msg.getOldModelCode().equals(model.getModelCode())) {
            String oldKey = buildTenantModelKey(msg.getTenantId(), msg.getOldModelCode());
            redisTemplate.delete(oldKey);
            log.debug("[ModelSyncConsumer] Deleted old key: {}", oldKey);
        }
        syncToRedis(model);
    }

    private void handleDelete(ModelSyncMessage msg) {
        String key = buildTenantModelKey(msg.getTenantId(), msg.getModelCode());
        redisTemplate.delete(key);
        log.debug("[ModelSyncConsumer] Deleted Redis key: {}", key);
    }

    private void syncToRedis(LlmModelCatalog model) {
        String key = buildTenantModelKey(model.getTenantId(), model.getModelCode());
        Map<String, String> map = new HashMap<>();
        map.put("litellm_model", StringUtils.defaultString(model.getLitellmModel(), model.getModelCode()));
        String apiKey = decryptApiKey(model.getApiKey());
        if (apiKey != null) map.put("api_key", apiKey);
        if (StringUtils.isNotBlank(model.getApiBase()))  map.put("api_base", model.getApiBase());
        if (StringUtils.isNotBlank(model.getProtocol()))  map.put("protocol", model.getProtocol());
        map.put("_checksum", computeChecksum(model));
        redisTemplate.opsForHash().putAll(key, map);
        log.info("[ModelSyncConsumer] Synced model config to Redis: key={}", key);
    }

    private String decryptApiKey(String encrypted) {
        if (StringUtils.isBlank(encrypted)) return null;
        try { return aesEncryptor.decrypt(encrypted); }
        catch (Exception e) {
            log.error("[ModelSyncConsumer] Failed to decrypt api_key: {}", e.getMessage());
            return null;
        }
    }

    private static String computeChecksum(LlmModelCatalog model) {
        String raw = StringUtils.defaultString(model.getLitellmModel())
            + "|" + StringUtils.defaultString(model.getApiKey())
            + "|" + StringUtils.defaultString(model.getApiBase())
            + "|" + StringUtils.defaultString(model.getProtocol());
        return Integer.toHexString(raw.hashCode());
    }

    private static String buildTenantModelKey(String tenantId, String modelCode) {
        if (StringUtils.isBlank(tenantId)) tenantId = "000000";
        return "tenant:" + tenantId + ":model:" + modelCode;
    }
}
