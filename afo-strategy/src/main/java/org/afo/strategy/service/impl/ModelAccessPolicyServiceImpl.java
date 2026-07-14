package org.afo.strategy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.strategy.domain.ModelAccessPolicy;
import org.afo.strategy.domain.vo.ModelAccessPolicyVo;
import org.afo.strategy.mapper.ModelAccessPolicyMapper;
import org.afo.strategy.service.IModelAccessPolicyService;
import org.afo.strategy.support.ModelCatalogLookup;
import org.afo.strategy.support.PolicyJsonCodec;
import org.afo.strategy.support.StrategyTenantResolver;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 企业模型准入策略服务实现。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ModelAccessPolicyServiceImpl implements IModelAccessPolicyService {

    private static final String STATUS_ENABLED = "0";
    private static final Set<String> DEFAULT_MODES = Set.of("ALLOW_UNLISTED", "DENY_UNLISTED");

    private final ModelAccessPolicyMapper modelAccessPolicyMapper;
    private final ModelCatalogLookup modelCatalogLookup;
    private final StrategyTenantResolver tenantResolver;
    private final PolicyJsonCodec policyJsonCodec;

    @Override
    public ModelAccessPolicyVo getActivePolicy(String tenantId) {
        String resolvedTenantId = tenantResolver.resolve(tenantId);
        Date now = new Date();
        List<ModelAccessPolicy> policies = modelAccessPolicyMapper.selectList(
            Wrappers.lambdaQuery(ModelAccessPolicy.class)
                .eq(ModelAccessPolicy::getTenantId, resolvedTenantId)
                .eq(ModelAccessPolicy::getStatus, STATUS_ENABLED)
                .eq(ModelAccessPolicy::getDelFlag, "0")
                .and(wrapper -> wrapper.isNull(ModelAccessPolicy::getEffectiveStart)
                    .or()
                    .le(ModelAccessPolicy::getEffectiveStart, now))
                .and(wrapper -> wrapper.isNull(ModelAccessPolicy::getEffectiveEnd)
                    .or()
                    .ge(ModelAccessPolicy::getEffectiveEnd, now))
                .orderByDesc(ModelAccessPolicy::getUpdateTime)
                .orderByDesc(ModelAccessPolicy::getCreateTime)
                .last("LIMIT 1"));
        if (policies.isEmpty()) {
            return null;
        }
        return toVo(policies.get(0));
    }

    @Override
    public TableDataInfo<ModelAccessPolicyVo> queryPageList(ModelAccessPolicy policy, PageQuery pageQuery) {
        LambdaQueryWrapper<ModelAccessPolicy> lqw = buildQueryWrapper(policy);
        Page<ModelAccessPolicyVo> result = modelAccessPolicyMapper.selectVoPage(pageQuery.build(), lqw);
        result.getRecords().forEach(this::fillModelSummary);
        return TableDataInfo.build(result);
    }

    @Override
    public ModelAccessPolicyVo queryById(Long policyId) {
        ModelAccessPolicyVo vo = modelAccessPolicyMapper.selectVoById(policyId);
        fillModelSummary(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(ModelAccessPolicy policy) {
        fillDefaults(policy);
        validateForSave(policy);
        modelAccessPolicyMapper.insert(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ModelAccessPolicy policy) {
        if (policy.getPolicyId() == null) {
            throw new ServiceException("策略 ID 不能为空");
        }
        ModelAccessPolicy existing = modelAccessPolicyMapper.selectById(policy.getPolicyId());
        if (existing == null) {
            throw new ServiceException("模型准入策略不存在");
        }
        fillDefaults(policy);
        validateForSave(policy);
        modelAccessPolicyMapper.updateById(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long policyId, String status) {
        ModelAccessPolicy policy = modelAccessPolicyMapper.selectById(policyId);
        if (policy == null) {
            throw new ServiceException("模型准入策略不存在");
        }
        policy.setStatus(status);
        validateForSave(policy);
        modelAccessPolicyMapper.updateById(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return;
        }
        modelAccessPolicyMapper.deleteBatchIds(policyIds);
    }

    @Override
    public List<ModelAccessPolicyVo> queryList(ModelAccessPolicy policy) {
        List<ModelAccessPolicyVo> list = modelAccessPolicyMapper.selectVoList(buildQueryWrapper(policy));
        list.forEach(this::fillModelSummary);
        return list;
    }

    private void validateForSave(ModelAccessPolicy policy) {
        if (StringUtils.isBlank(policy.getPolicyName())) {
            throw new ServiceException("配置名称不能为空");
        }
        if (!DEFAULT_MODES.contains(policy.getDefaultMode())) {
            throw new ServiceException("默认准入模式不合法");
        }
        if (policy.getEffectiveStart() != null
            && policy.getEffectiveEnd() != null
            && policy.getEffectiveEnd().before(policy.getEffectiveStart())) {
            throw new ServiceException("生效结束时间不能早于生效开始时间");
        }

        List<String> allowedModels = parseRequiredModelCodes(policy.getAllowedModels());
        List<String> deniedModels = parseRequiredModelCodes(policy.getDeniedModels());
        Set<String> duplicates = new HashSet<>(allowedModels);
        duplicates.retainAll(deniedModels);
        if (!duplicates.isEmpty()) {
            throw new ServiceException("同一模型不能同时出现在允许模型和禁止模型中");
        }
        modelCatalogLookup.requireModelCodesExist(allowedModels);
        modelCatalogLookup.requireModelCodesExist(deniedModels);

        if (STATUS_ENABLED.equals(policy.getStatus())) {
            validateActiveWindowNotOverlapped(policy);
        }
    }

    private void validateActiveWindowNotOverlapped(ModelAccessPolicy policy) {
        List<ModelAccessPolicy> candidates = modelAccessPolicyMapper.selectList(
            Wrappers.lambdaQuery(ModelAccessPolicy.class)
                .eq(ModelAccessPolicy::getTenantId, policy.getTenantId())
                .eq(ModelAccessPolicy::getStatus, STATUS_ENABLED)
                .eq(ModelAccessPolicy::getDelFlag, "0")
                .ne(policy.getPolicyId() != null, ModelAccessPolicy::getPolicyId, policy.getPolicyId()));
        boolean overlapped = candidates.stream().anyMatch(candidate -> isWindowOverlapped(policy, candidate));
        if (overlapped) {
            throw new ServiceException("同一租户同一时间只能存在一条启用的模型准入配置");
        }
    }

    private boolean isWindowOverlapped(ModelAccessPolicy left, ModelAccessPolicy right) {
        Date leftStart = left.getEffectiveStart();
        Date leftEnd = left.getEffectiveEnd();
        Date rightStart = right.getEffectiveStart();
        Date rightEnd = right.getEffectiveEnd();
        boolean leftStartsBeforeRightEnds = rightEnd == null || leftStart == null || !leftStart.after(rightEnd);
        boolean rightStartsBeforeLeftEnds = leftEnd == null || rightStart == null || !rightStart.after(leftEnd);
        return leftStartsBeforeRightEnds && rightStartsBeforeLeftEnds;
    }

    private LambdaQueryWrapper<ModelAccessPolicy> buildQueryWrapper(ModelAccessPolicy policy) {
        LambdaQueryWrapper<ModelAccessPolicy> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(policy.getTenantId()), ModelAccessPolicy::getTenantId, policy.getTenantId());
        lqw.like(StringUtils.isNotBlank(policy.getPolicyName()), ModelAccessPolicy::getPolicyName, policy.getPolicyName());
        lqw.eq(StringUtils.isNotBlank(policy.getDefaultMode()), ModelAccessPolicy::getDefaultMode, policy.getDefaultMode());
        lqw.eq(StringUtils.isNotBlank(policy.getStatus()), ModelAccessPolicy::getStatus, policy.getStatus());
        lqw.orderByDesc(ModelAccessPolicy::getUpdateTime);
        lqw.orderByDesc(ModelAccessPolicy::getCreateTime);
        return lqw;
    }

    private void fillDefaults(ModelAccessPolicy policy) {
        policy.setTenantId(tenantResolver.resolve(policy.getTenantId()));
        if (StringUtils.isBlank(policy.getDefaultMode())) {
            policy.setDefaultMode("ALLOW_UNLISTED");
        }
        if (StringUtils.isBlank(policy.getAllowedModels())) {
            policy.setAllowedModels("[]");
        }
        if (StringUtils.isBlank(policy.getDeniedModels())) {
            policy.setDeniedModels("[]");
        }
        if (StringUtils.isBlank(policy.getStatus())) {
            policy.setStatus(STATUS_ENABLED);
        }
    }

    private List<String> parseRequiredModelCodes(String json) {
        return policyJsonCodec.parseRequiredStringList(json, "模型列表必须是 JSON 字符串数组");
    }

    private ModelAccessPolicyVo toVo(ModelAccessPolicy policy) {
        ModelAccessPolicyVo vo = new ModelAccessPolicyVo();
        BeanUtils.copyProperties(policy, vo);
        fillModelSummary(vo);
        return vo;
    }

    private void fillModelSummary(ModelAccessPolicyVo vo) {
        if (vo == null) {
            return;
        }
        List<String> allowedModels = parseRequiredModelCodes(vo.getAllowedModels());
        List<String> deniedModels = parseRequiredModelCodes(vo.getDeniedModels());
        vo.setAllowedModelCount(allowedModels.size());
        vo.setDeniedModelCount(deniedModels.size());

        Set<String> modelCodes = new HashSet<>();
        modelCodes.addAll(allowedModels);
        modelCodes.addAll(deniedModels);
        if (modelCodes.isEmpty()) {
            vo.setAllowedModelDetails(Collections.emptyList());
            vo.setDeniedModelDetails(Collections.emptyList());
            return;
        }

        vo.setAllowedModelDetails(modelCatalogLookup.accessModelDetails(allowedModels));
        vo.setDeniedModelDetails(modelCatalogLookup.accessModelDetails(deniedModels));
    }
}
