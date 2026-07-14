package org.afo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.core.utils.crypto.AesEncryptor;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "model.sync.reconciliation.enabled",
    havingValue = "true", matchIfMissing = true)
public class ModelSyncReconciliationService {

    private final LlmModelCatalogMapper catalogMapper;
    private final StringRedisTemplate redisTemplate;
    private final AesEncryptor aesEncryptor;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOnStartup() {
        log.info("[Reconcile] Application ready, warming up Redis from DB...");
        try {
            reconcile();
        } catch (Exception e) {
            log.error("[Reconcile] Startup warm-up failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRateString = "${model.sync.reconciliation.interval:3600000}")
    public void reconcile() {
        log.info("[Reconcile] Starting DB-Redis model sync reconciliation...");

        Set<String> activeRedisKeys = new HashSet<>();
        int repaired = 0;

        for (LlmModelCatalog model : catalogMapper.selectList(
                new LambdaQueryWrapper<LlmModelCatalog>()
                    .eq(LlmModelCatalog::getDelFlag, "0")
                    .eq(LlmModelCatalog::getStatus, "0"))) {

            String key = buildTenantModelKey(model.getTenantId(), model.getModelCode());
            activeRedisKeys.add(key);

            String redisChecksum = (String) redisTemplate.opsForHash().get(key, "_checksum");
            String dbChecksum = computeChecksum(model);

            if (redisChecksum == null || !redisChecksum.equals(dbChecksum)) {
                log.warn("[Reconcile] Inconsistent model: {} (tenant={}), repairing...",
                    model.getModelCode(), model.getTenantId());
                syncToRedis(model);
                repaired++;
            }
        }

        int orphanCount = 0;
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("tenant:*:model:*").count(100).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (!activeRedisKeys.contains(key)) {
                    redisTemplate.delete(key);
                    orphanCount++;
                }
            }
        }

        log.info("[Reconcile] Done: repaired={}, orphanKeysCleaned={}", repaired, orphanCount);
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
    }

    private String decryptApiKey(String encrypted) {
        if (StringUtils.isBlank(encrypted)) return null;
        try { return aesEncryptor.decrypt(encrypted); }
        catch (Exception e) { return null; }
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
