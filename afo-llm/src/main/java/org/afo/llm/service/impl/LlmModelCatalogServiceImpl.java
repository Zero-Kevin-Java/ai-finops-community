package org.afo.llm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.utils.MapstructUtils;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.core.utils.crypto.AesEncryptor;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.rabbitmq.config.ModelSyncQueueConfig;
import org.afo.common.rabbitmq.message.ModelSyncMessage;
import org.afo.common.tenant.helper.TenantHelper;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.domain.LlmProvider;
import org.afo.llm.domain.bo.LlmModelCatalogBo;
import org.afo.llm.domain.vo.LlmModelCatalogVo;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.afo.llm.mapper.LlmProviderMapper;
import org.afo.llm.service.ILlmModelCatalogService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM 模型目录服务实现。
 *
 * @author afo
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class LlmModelCatalogServiceImpl implements ILlmModelCatalogService {

    private final LlmModelCatalogMapper baseMapper;
    private final StringRedisTemplate redisTemplate;
    private final AesEncryptor aesEncryptor;
    private final RabbitTemplate rabbitTemplate;
    private final LlmProviderMapper providerMapper;
    private final boolean rabbitmqEnabled;

    @Override
    public LlmModelCatalogVo queryById(Long modelId) {
        LlmModelCatalogVo vo = baseMapper.selectVoById(modelId);
        return maskApiKey(vo);
    }

    @Override
    public TableDataInfo<LlmModelCatalogVo> queryPageList(LlmModelCatalogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LlmModelCatalog> lqw = buildQueryWrapper(bo);
        Page<LlmModelCatalogVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        result.getRecords().forEach(this::maskApiKey);
        return TableDataInfo.build(result);
    }

    @Override
    public List<LlmModelCatalogVo> queryList(LlmModelCatalogBo bo) {
        LambdaQueryWrapper<LlmModelCatalog> lqw = buildQueryWrapper(bo);
        List<LlmModelCatalogVo> list = baseMapper.selectVoList(lqw);
        list.forEach(this::maskApiKey);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(LlmModelCatalogBo bo) {
        LlmModelCatalog add = MapstructUtils.convert(bo, LlmModelCatalog.class);
        fillDefaultValues(add);
        fillProviderByModelCode(add);

        add.setLitellmModel(resolveLitellmModel(add.getLitellmModel(), add.getModelCode(), add.getProtocol()));
        // Anthropic 协议设置默认 api_base
        if ("anthropic".equals(add.getProtocol()) && StringUtils.isBlank(add.getApiBase())) {
            add.setApiBase("https://api.anthropic.com");
        }
        // api_key 加密后存入 DB
        if (StringUtils.isNotBlank(add.getApiKey())) {
            add.setApiKey(aesEncryptor.encrypt(add.getApiKey()));
        }

        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setModelId(add.getModelId());
            if (rabbitmqEnabled) {
                publishModelSyncAfterCommit("CREATE", add, null);
            } else {
                syncModelConfigToRedis(add);
            }
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(LlmModelCatalogBo bo) {
        // 必须先查 DB 获取 tenantId
        LlmModelCatalog existing = baseMapper.selectById(bo.getModelId());
        if (existing == null) {
            log.warn("[ModelCatalog] Model not found for update: id={}", bo.getModelId());
            return false;
        }

        LlmModelCatalog update = MapstructUtils.convert(bo, LlmModelCatalog.class);

        update.setLitellmModel(resolveLitellmModel(update.getLitellmModel(), update.getModelCode(), update.getProtocol()));

        // 判断 modelCode 是否变更（仅记录，不直接删 Redis，由 MQ 消费者或降级路径处理）
        boolean modelCodeChanged = StringUtils.isNotBlank(update.getModelCode())
            && !update.getModelCode().equals(existing.getModelCode());

        // 用 LambdaUpdateWrapper 按字段 set，避免 null 覆盖
        LambdaUpdateWrapper<LlmModelCatalog> uw = new LambdaUpdateWrapper<LlmModelCatalog>()
            .eq(LlmModelCatalog::getModelId, bo.getModelId());

        if (StringUtils.isNotBlank(update.getModelCode())) {
            uw.set(LlmModelCatalog::getModelCode, update.getModelCode());
        }
        if (StringUtils.isNotBlank(update.getDisplayName())) {
            uw.set(LlmModelCatalog::getDisplayName, update.getDisplayName());
        }
        if (StringUtils.isNotBlank(update.getProvider())) {
            uw.set(LlmModelCatalog::getProvider, update.getProvider());
        }
        if (StringUtils.isNotBlank(update.getSupplier())) {
            uw.set(LlmModelCatalog::getSupplier, update.getSupplier());
        }
        if (StringUtils.isNotBlank(update.getLitellmModel())) {
            uw.set(LlmModelCatalog::getLitellmModel, update.getLitellmModel());
        }
        if (StringUtils.isNotBlank(update.getProtocol())) {
            uw.set(LlmModelCatalog::getProtocol, update.getProtocol());
        }
        // api_key 仅当用户填写时才更新
        if (StringUtils.isNotBlank(update.getApiKey())) {
            uw.set(LlmModelCatalog::getApiKey, aesEncryptor.encrypt(update.getApiKey()));
        }
        if (StringUtils.isNotBlank(update.getApiBase())) {
            uw.set(LlmModelCatalog::getApiBase, update.getApiBase());
        }
        if (StringUtils.isNotBlank(update.getModelType())) {
            uw.set(LlmModelCatalog::getModelType, update.getModelType());
        }
        if (StringUtils.isNotBlank(update.getStatus())) {
            uw.set(LlmModelCatalog::getStatus, update.getStatus());
        }
        if (StringUtils.isNotBlank(update.getRemark())) {
            uw.set(LlmModelCatalog::getRemark, update.getRemark());
        }

        baseMapper.update(uw);

        // DB 更新成功后，从 DB 重新读取最新数据，同步到 Redis
        LlmModelCatalog refreshed = baseMapper.selectById(bo.getModelId());
        if (refreshed != null) {
            if (rabbitmqEnabled) {
                if (modelCodeChanged) {
                    publishModelSyncAfterCommit("DELETE", existing, null);
                }
                publishModelSyncAfterCommit("UPDATE", refreshed, modelCodeChanged ? existing.getModelCode() : null);
            } else {
                String oldKey = modelKey(existing.getTenantId(), existing.getModelCode());
                if (modelCodeChanged) {
                    redisTemplate.delete(oldKey);
                }
                syncModelConfigToRedis(refreshed);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<LlmModelCatalog> models = baseMapper.selectBatchIds(ids);
        boolean flag = baseMapper.deleteByIds(ids) > 0;
        if (flag) {
            for (LlmModelCatalog m : models) {
                if (rabbitmqEnabled) {
                    publishModelSyncAfterCommit("DELETE", m, null);
                } else {
                    redisTemplate.delete(modelKey(m.getTenantId(), m.getModelCode()));
                }
            }
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(Long modelId, String status) {
        LlmModelCatalog existing = baseMapper.selectById(modelId);
        boolean flag = baseMapper.update(null,
            new LambdaUpdateWrapper<LlmModelCatalog>()
                .set(LlmModelCatalog::getStatus, status)
                .eq(LlmModelCatalog::getModelId, modelId)) > 0;

        // 禁用模型时清除 Redis 缓存
        if (flag && existing != null && "1".equals(status)) {
            if (rabbitmqEnabled) {
                publishModelSyncAfterCommit("DISABLE", existing, null);
            } else {
                redisTemplate.delete(modelKey(existing.getTenantId(), existing.getModelCode()));
            }
        }
        return flag;
    }

    @Override
    public boolean checkModelCodeUnique(LlmModelCatalogBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<LlmModelCatalog>()
            .eq(LlmModelCatalog::getModelCode, bo.getModelCode())
            .ne(ObjectUtil.isNotNull(bo.getModelId()), LlmModelCatalog::getModelId, bo.getModelId()));
        return !exist;
    }

    @Override
    public List<Map<String, Object>> listOptions() {
        List<LlmModelCatalog> list = baseMapper.selectList(
            Wrappers.lambdaQuery(LlmModelCatalog.class)
                .eq(LlmModelCatalog::getDelFlag, "0")
                .orderByDesc(LlmModelCatalog::getCreateTime)
                .select(
                    LlmModelCatalog::getModelId,
                    LlmModelCatalog::getModelCode,
                    LlmModelCatalog::getDisplayName,
                    LlmModelCatalog::getProvider,
                    LlmModelCatalog::getSupplier));
        return list.stream().map(m -> {
            Map<String, Object> opt = new HashMap<>();
            String label = StringUtils.isNotBlank(m.getDisplayName())
                ? m.getDisplayName()
                : m.getModelCode();
            opt.put("modelId", m.getModelId());
            opt.put("label", label);
            opt.put("value", m.getModelCode());
            opt.put("modelCode", m.getModelCode());
            opt.put("displayName", m.getDisplayName());
            opt.put("provider", m.getProvider());
            opt.put("supplier", m.getSupplier());
            return opt;
        }).collect(Collectors.toList());
    }

    @Override
    public LlmModelCatalog getModelConfig(String tenantId, String modelCode) {
        return baseMapper.selectOne(
            Wrappers.lambdaQuery(LlmModelCatalog.class)
                .eq(LlmModelCatalog::getTenantId, tenantId)
                .eq(LlmModelCatalog::getModelCode, modelCode)
                .eq(LlmModelCatalog::getDelFlag, "0")
                .eq(LlmModelCatalog::getStatus, "0")
                .last("LIMIT 1"));
    }

    private void publishModelSyncAfterCommit(String action, LlmModelCatalog model, String oldModelCode) {
        if (!rabbitmqEnabled) return;

        ModelSyncMessage msg = new ModelSyncMessage(
            action, model.getTenantId(), model.getModelCode(), model.getModelId(), oldModelCode);

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        rabbitTemplate.convertAndSend(
                            ModelSyncQueueConfig.EXCHANGE_NAME, ModelSyncQueueConfig.ROUTING_KEY, msg);
                        log.debug("[ModelSync] Published {} for model {} (tenant={})",
                            action, model.getModelCode(), model.getTenantId());
                    } catch (Exception e) {
                        log.error("[ModelSync] Failed to publish {} for model {}: {}",
                            action, model.getModelCode(), e.getMessage());
                    }
                }
            });
    }

    private LambdaQueryWrapper<LlmModelCatalog> buildQueryWrapper(LlmModelCatalogBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LlmModelCatalog> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getModelCode()), LlmModelCatalog::getModelCode, bo.getModelCode());
        lqw.like(StringUtils.isNotBlank(bo.getDisplayName()), LlmModelCatalog::getDisplayName, bo.getDisplayName());
        lqw.eq(StringUtils.isNotBlank(bo.getProvider()), LlmModelCatalog::getProvider, bo.getProvider());
        lqw.like(StringUtils.isNotBlank(bo.getSupplier()), LlmModelCatalog::getSupplier, bo.getSupplier());
        lqw.eq(StringUtils.isNotBlank(bo.getModelType()), LlmModelCatalog::getModelType, bo.getModelType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), LlmModelCatalog::getStatus, bo.getStatus());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            LlmModelCatalog::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(LlmModelCatalog::getCreateTime);
        return lqw;
    }

    private void fillDefaultValues(LlmModelCatalog model) {
        if (StringUtils.isBlank(model.getStatus())) {
            model.setStatus("0");
        }
        if (StringUtils.isBlank(model.getProtocol())) {
            model.setProtocol("openai");
        }
    }

    private void fillProviderByModelCode(LlmModelCatalog model) {
        if (StringUtils.isNotBlank(model.getProvider()) || StringUtils.isBlank(model.getModelCode())) {
            return;
        }

        findMatchedProvider(model.getModelCode()).map(LlmProvider::getProviderName).ifPresent(model::setProvider);
    }

    private java.util.Optional<LlmProvider> findMatchedProvider(String modelCode) {
        String normalizedModelCode = modelCode.trim().toLowerCase(Locale.ROOT);
        return providerMapper.selectList(Wrappers.lambdaQuery(LlmProvider.class)
                .eq(LlmProvider::getStatus, "0")
                .isNotNull(LlmProvider::getModelPrefixes))
            .stream()
            .flatMap(provider -> splitModelPrefixes(provider.getModelPrefixes()).stream()
                .filter(prefix -> normalizedModelCode.startsWith(prefix.toLowerCase(Locale.ROOT)))
                .map(prefix -> new ProviderPrefixMatch(provider, prefix.length())))
            .max(Comparator.comparingInt(ProviderPrefixMatch::prefixLength)
                .thenComparingInt(match -> -safeSortOrder(match.provider())))
            .map(ProviderPrefixMatch::provider);
    }

    private List<String> splitModelPrefixes(String modelPrefixes) {
        if (StringUtils.isBlank(modelPrefixes)) {
            return List.of();
        }

        Set<String> prefixes = new LinkedHashSet<>();
        for (String item : modelPrefixes.split("[,，;；、\\n\\r\\t]+")) {
            String prefix = item.trim();
            if (StringUtils.isNotBlank(prefix)) {
                prefixes.add(prefix);
            }
        }
        return List.copyOf(prefixes);
    }

    private int safeSortOrder(LlmProvider provider) {
        return provider.getSortOrder() == null ? Integer.MAX_VALUE : provider.getSortOrder();
    }

    static String resolveLitellmModel(String litellmModel, String modelCode, String protocol) {
        if (StringUtils.isNotBlank(litellmModel) || StringUtils.isBlank(modelCode)) {
            return litellmModel;
        }
        if (modelCode.contains("/")) {
            return modelCode;
        }
        String prefix = "anthropic".equals(protocol) ? "anthropic/" : "openai/";
        return prefix + modelCode;
    }

    private record ProviderPrefixMatch(LlmProvider provider, int prefixLength) {
    }

    /**
     * 同步模型配置到 Redis（解密后存明文）。
     */
    private void syncModelConfigToRedis(LlmModelCatalog model) {
        String key = modelKey(model.getTenantId(), model.getModelCode());
        Map<String, String> map = new HashMap<>();

        map.put("litellm_model", StringUtils.defaultString(model.getLitellmModel(), model.getModelCode()));

        // Redis 中存明文（供网关读取转发），从 DB 读出的密文需要解密
        String apiKey = decryptApiKey(model.getApiKey());
        if (StringUtils.isNotBlank(apiKey)) {
            map.put("api_key", apiKey);
        }
        if (StringUtils.isNotBlank(model.getApiBase())) {
            map.put("api_base", model.getApiBase());
        }
        if (StringUtils.isNotBlank(model.getProtocol())) {
            map.put("protocol", model.getProtocol());
        }

        map.put("_checksum", computeChecksum(model));

        redisTemplate.opsForHash().putAll(key, map);
        log.debug("[ModelCatalog] Synced model config to Redis: key={}", key);
    }

    private String computeChecksum(LlmModelCatalog model) {
        String raw = StringUtils.defaultString(model.getLitellmModel())
            + "|" + StringUtils.defaultString(model.getApiKey())
            + "|" + StringUtils.defaultString(model.getApiBase())
            + "|" + StringUtils.defaultString(model.getProtocol());
        return Integer.toHexString(raw.hashCode());
    }

    private LlmModelCatalogVo maskApiKey(LlmModelCatalogVo vo) {
        if (vo != null) {
            vo.setApiKey(null);
        }
        return vo;
    }

    private String decryptApiKey(String encrypted) {
        if (StringUtils.isBlank(encrypted)) {
            return null;
        }
        try {
            return aesEncryptor.decrypt(encrypted);
        } catch (Exception e) {
            log.error("[ModelCatalog] Failed to decrypt api_key: {}", e.getMessage());
            return null;
        }
    }

    private String modelKey(String tenantId, String modelCode) {
        if (StringUtils.isBlank(tenantId)) {
            tenantId = TenantHelper.getTenantId();
        }
        if (StringUtils.isBlank(tenantId)) {
            tenantId = "000000";
        }
        return "tenant:" + tenantId + ":model:" + modelCode;
    }
}
