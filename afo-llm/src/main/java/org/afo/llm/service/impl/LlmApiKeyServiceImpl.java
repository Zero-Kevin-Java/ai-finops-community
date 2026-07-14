package org.afo.llm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.core.utils.MapstructUtils;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.LlmAppClient;
import org.afo.llm.domain.LlmApiKey;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.domain.bo.LlmApiKeyBo;
import org.afo.llm.domain.vo.LlmApiKeyVo;
import org.afo.llm.mapper.LlmAppClientMapper;
import org.afo.llm.mapper.LlmApiKeyMapper;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.afo.llm.service.ILlmApiKeyService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM 业务 API Key 服务实现。
 *
 * @author afo
 */
@RequiredArgsConstructor
@Service
public class LlmApiKeyServiceImpl implements ILlmApiKeyService {

    private static final String KEY_PREFIX = "sk_";
    private static final String LEGACY_KEY_PREFIX = "afo_";
    private static final String REFRESH_TOPIC = "gateway:cache:refresh";
    private static final Set<String> ALLOWED_STATUS = Set.of("0", "1");

    private final LlmApiKeyMapper baseMapper;
    private final LlmModelCatalogMapper modelCatalogMapper;
    private final LlmAppClientMapper appClientMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询 API Key 详情。
     *
     * @param keyId API Key ID
     * @return API Key 详情
     */
    @Override
    public LlmApiKeyVo queryById(Long keyId) {
        LlmApiKeyVo vo = maskApiKey(baseMapper.selectVoById(keyId));
        if (vo != null) {
            enrichNames(Collections.singletonList(vo));
        }
        return vo;
    }

    /**
     * 分页查询 API Key。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return API Key 分页
     */
    @Override
    public TableDataInfo<LlmApiKeyVo> queryPageList(LlmApiKeyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LlmApiKey> lqw = buildQueryWrapper(bo);
        Page<LlmApiKeyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        result.getRecords().forEach(this::maskApiKey);
        enrichNames(result.getRecords());
        return TableDataInfo.build(result);
    }

    /**
     * 查询 API Key 列表。
     *
     * @param bo 查询条件
     * @return API Key 列表
     */
    @Override
    public List<LlmApiKeyVo> queryList(LlmApiKeyBo bo) {
        LambdaQueryWrapper<LlmApiKey> lqw = buildQueryWrapper(bo);
        List<LlmApiKeyVo> list = baseMapper.selectVoList(lqw);
        list.forEach(this::maskApiKey);
        enrichNames(list);
        return list;
    }

    /**
     * 新增 API Key。
     *
     * @param bo API Key
     * @return 仅返回一次的明文 Key
     */
    @Override
    public String insertByBo(LlmApiKeyBo bo) {
        LlmApiKey add = MapstructUtils.convert(bo, LlmApiKey.class);
        fillDefaultValues(add);
        validateModelScope(add.getKeyScope());
        String plainKey = generateUniquePlainKey();
        add.setKeyPrefix(plainKey.substring(0, 24));
        add.setKeyHash(SecureUtil.sha256(plainKey));
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setKeyId(add.getKeyId());
            return plainKey;
        }
        throw new IllegalStateException("新增 API Key 失败");
    }

    /**
     * 修改 API Key。
     *
     * @param bo API Key
     * @return 是否成功
     */
    @Override
    public Boolean updateByBo(LlmApiKeyBo bo) {
        LlmApiKey existing = baseMapper.selectById(bo.getKeyId());
        if (existing == null) {
            throw new ServiceException("API Key 不存在");
        }
        LlmApiKey update = buildUpdateEntity(bo);
        fillDefaultValues(update);
        update.setKeyPrefix(existing.getKeyPrefix());
        update.setKeyHash(existing.getKeyHash());
        validateModelScope(update.getKeyScope());
        boolean success = baseMapper.updateById(update) > 0;
        if (success) {
            publishApiKeyRefresh(existing.getKeyHash());
        }
        return success;
    }

    /**
     * 批量删除 API Key。
     *
     * @param ids API Key ID集合
     * @param isValid 是否校验
     * @return 是否成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<LlmApiKey> existingKeys = baseMapper.selectBatchIds(ids);
        boolean success = baseMapper.deleteByIds(ids) > 0;
        if (success) {
            existingKeys.forEach(apiKey -> publishApiKeyRefresh(apiKey.getKeyHash()));
        }
        return success;
    }

    /**
     * 修改 API Key 状态。
     *
     * @param keyId API Key ID
     * @param status 状态
     * @return 是否成功
     */
    @Override
    public Boolean updateStatus(Long keyId, String status) {
        validateStatus(status);
        LlmApiKey existing = baseMapper.selectById(keyId);
        boolean success = baseMapper.update(null,
            new LambdaUpdateWrapper<LlmApiKey>()
                .set(LlmApiKey::getStatus, status)
                .eq(LlmApiKey::getKeyId, keyId)) > 0;
        if (success && existing != null) {
            publishApiKeyRefresh(existing.getKeyHash());
        }
        return success;
    }

    private LambdaQueryWrapper<LlmApiKey> buildQueryWrapper(LlmApiKeyBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LlmApiKey> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjectUtil.isNotNull(bo.getClientId()), LlmApiKey::getClientId, bo.getClientId());
        lqw.eq(ObjectUtil.isNotNull(bo.getOwnerUserId()), LlmApiKey::getOwnerUserId, bo.getOwnerUserId());
        lqw.like(StringUtils.isNotBlank(bo.getKeyName()), LlmApiKey::getKeyName, bo.getKeyName());
        lqw.like(StringUtils.isNotBlank(bo.getKeyPrefix()), LlmApiKey::getKeyPrefix, bo.getKeyPrefix());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), LlmApiKey::getStatus, bo.getStatus());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            LlmApiKey::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(LlmApiKey::getCreateTime);
        return lqw;
    }

    private void fillDefaultValues(LlmApiKey apiKey) {
        if (StringUtils.isBlank(apiKey.getStatus())) {
            apiKey.setStatus("0");
        }
        validateStatus(apiKey.getStatus());
    }

    private LlmApiKey buildUpdateEntity(LlmApiKeyBo bo) {
        LlmApiKey update = new LlmApiKey();
        update.setKeyId(bo.getKeyId());
        update.setClientId(bo.getClientId());
        update.setOwnerUserId(bo.getOwnerUserId());
        update.setKeyName(bo.getKeyName());
        update.setKeyScope(bo.getKeyScope());
        update.setExpireTime(bo.getExpireTime());
        update.setStatus(bo.getStatus());
        update.setRemark(bo.getRemark());
        return update;
    }

    private void enrichNames(List<LlmApiKeyVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> clientIds = vos.stream()
            .map(LlmApiKeyVo::getClientId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> appNameMap = Collections.emptyMap();
        if (!clientIds.isEmpty()) {
            appNameMap = appClientMapper.selectBatchIds(clientIds).stream()
                .collect(Collectors.toMap(LlmAppClient::getClientId, LlmAppClient::getAppName, (a, b) -> a));
        }

        for (LlmApiKeyVo vo : vos) {
            vo.setAppName(appNameMap.get(vo.getClientId()));
        }
    }

    private void validateStatus(String status) {
        if (StringUtils.isNotBlank(status) && !ALLOWED_STATUS.contains(status)) {
            throw new ServiceException("API Key 状态仅支持正常或停用");
        }
    }

    private void validateModelScope(String keyScope) {
        Set<String> modelCodes = parseModelScope(keyScope);
        if (modelCodes.isEmpty()) {
            throw new ServiceException("授权模型不能为空");
        }
        if (modelCodes.contains("*")) {
            return;
        }
        for (String modelCode : modelCodes) {
            boolean exists = modelCatalogMapper.exists(new LambdaQueryWrapper<LlmModelCatalog>()
                .eq(LlmModelCatalog::getModelCode, modelCode)
                .eq(LlmModelCatalog::getDelFlag, "0"));
            if (!exists) {
                throw new ServiceException("授权模型不存在：{}", modelCode);
            }
        }
    }

    private Set<String> parseModelScope(String keyScope) {
        if (StringUtils.isBlank(keyScope)) {
            return Set.of();
        }
        String trimmed = keyScope.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                return Arrays.stream(objectMapper.readValue(trimmed, String[].class))
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toSet());
            } catch (Exception e) {
                throw new ServiceException("授权模型必须是模型编码列表");
            }
        }
        return Arrays.stream(trimmed.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    private void publishApiKeyRefresh(String keyHash) {
        if (StringUtils.isNotBlank(keyHash)) {
            stringRedisTemplate.convertAndSend(REFRESH_TOPIC, "apikey:" + keyHash);
        }
    }

    private String generateUniquePlainKey() {
        for (int i = 0; i < 5; i++) {
            String plainKey = KEY_PREFIX + RandomUtil.randomString(48);
            String keyPrefix = plainKey.substring(0, 24);
            String keyHash = SecureUtil.sha256(plainKey);
            boolean exists = baseMapper.exists(new LambdaQueryWrapper<LlmApiKey>()
                .eq(LlmApiKey::getKeyPrefix, keyPrefix)
                .or()
                .eq(LlmApiKey::getKeyHash, keyHash));
            if (!exists) {
                return plainKey;
            }
        }
        throw new IllegalStateException("生成 API Key 失败，请重试");
    }

    private LlmApiKeyVo maskApiKey(LlmApiKeyVo vo) {
        if (vo == null || StringUtils.isBlank(vo.getKeyPrefix())) {
            return vo;
        }
        String keyPrefix = vo.getKeyPrefix();
        if (keyPrefix.startsWith(LEGACY_KEY_PREFIX)) {
            keyPrefix = keyPrefix.substring(LEGACY_KEY_PREFIX.length());
        }
        String visiblePrefix = keyPrefix.length() <= 4 ? "" : keyPrefix.substring(0, keyPrefix.length() - 4);
        vo.setKeyPrefix(visiblePrefix + "****");
        return vo;
    }
}
