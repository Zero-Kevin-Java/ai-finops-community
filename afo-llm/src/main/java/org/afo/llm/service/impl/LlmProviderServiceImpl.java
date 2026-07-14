package org.afo.llm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.utils.MapstructUtils;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.LlmProvider;
import org.afo.llm.domain.bo.LlmProviderBo;
import org.afo.llm.domain.vo.LlmProviderVo;
import org.afo.llm.mapper.LlmProviderMapper;
import org.afo.llm.service.ILlmProviderService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class LlmProviderServiceImpl implements ILlmProviderService {

    private final LlmProviderMapper baseMapper;

    @Override
    public LlmProviderVo queryById(Long providerId) {
        LlmProvider provider = baseMapper.selectById(providerId);
        return provider == null ? null : toProviderVo(provider);
    }

    @Override
    public TableDataInfo<LlmProviderVo> queryPageList(LlmProviderBo bo, PageQuery pageQuery) {
        Page<LlmProvider> page = pageQuery.build();
        List<LlmProvider> providers = baseMapper.selectList(page, buildQueryWrapper(bo));
        Page<LlmProviderVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(providers.stream().map(this::toProviderVo).toList());
        return TableDataInfo.build(result);
    }

    @Override
    public List<LlmProviderVo> queryList(LlmProviderBo bo) {
        return baseMapper.selectList(buildQueryWrapper(bo)).stream().map(this::toProviderVo).toList();
    }

    @Override
    public Boolean insertByBo(LlmProviderBo bo) {
        normalizeProviderBo(bo);
        LlmProvider add = MapstructUtils.convert(bo, LlmProvider.class);
        fillDefaultValues(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setProviderId(add.getProviderId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(LlmProviderBo bo) {
        normalizeProviderBo(bo);
        LlmProvider update = MapstructUtils.convert(bo, LlmProvider.class);
        fillDefaultValues(update);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean updateStatus(Long providerId, String status) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<LlmProvider>()
                .set(LlmProvider::getStatus, status)
                .eq(LlmProvider::getProviderId, providerId)) > 0;
    }

    @Override
    public List<Map<String, Object>> listOptions() {
        return baseMapper.selectList(Wrappers.lambdaQuery(LlmProvider.class)
                .eq(LlmProvider::getStatus, "0")
                .orderByAsc(LlmProvider::getSortOrder)
                .orderByAsc(LlmProvider::getProviderName))
            .stream().map(provider -> {
                Map<String, Object> opt = new HashMap<>();
                opt.put("label", provider.getProviderName());
                opt.put("value", provider.getProviderId());
                opt.put("providerName", provider.getProviderName());
                opt.put("logoSlug", provider.getLogoSlug());
                opt.put("modelPrefixes", provider.getModelPrefixes());
                return opt;
            }).collect(Collectors.toList());
    }

    @Override
    public LlmProviderVo matchByModelName(String modelName) {
        if (StringUtils.isBlank(modelName)) {
            return null;
        }

        String normalizedModelName = modelName.trim().toLowerCase(Locale.ROOT);
        return baseMapper.selectList(Wrappers.lambdaQuery(LlmProvider.class)
                .eq(LlmProvider::getStatus, "0")
                .isNotNull(LlmProvider::getModelPrefixes))
            .stream()
            .flatMap(provider -> splitModelPrefixes(provider.getModelPrefixes()).stream()
                .filter(prefix -> normalizedModelName.startsWith(prefix.toLowerCase(Locale.ROOT)))
                .map(prefix -> new ProviderPrefixMatch(provider, prefix.length())))
            .max(Comparator.comparingInt(ProviderPrefixMatch::prefixLength)
                .thenComparingInt(match -> -safeSortOrder(match.provider())))
            .map(match -> toProviderVo(match.provider()))
            .orElse(null);
    }

    private LambdaQueryWrapper<LlmProvider> buildQueryWrapper(LlmProviderBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LlmProvider> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getProviderName()), LlmProvider::getProviderName, bo.getProviderName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), LlmProvider::getStatus, bo.getStatus());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            LlmProvider::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByAsc(LlmProvider::getSortOrder).orderByDesc(LlmProvider::getCreateTime);
        return lqw;
    }

    private void fillDefaultValues(LlmProvider provider) {
        if (StringUtils.isBlank(provider.getStatus())) {
            provider.setStatus("0");
        }
        if (provider.getSortOrder() == null) {
            provider.setSortOrder(0);
        }
    }

    private void normalizeProviderBo(LlmProviderBo bo) {
        bo.setModelPrefixes(normalizeModelPrefixes(bo.getModelPrefixes()));
    }

    static String normalizeModelPrefixes(String modelPrefixes) {
        List<String> prefixes = splitModelPrefixes(modelPrefixes);
        if (prefixes.isEmpty()) {
            return null;
        }
        return String.join(",", prefixes);
    }

    private static List<String> splitModelPrefixes(String modelPrefixes) {
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

    private LlmProviderVo toProviderVo(LlmProvider provider) {
        LlmProviderVo vo = new LlmProviderVo();
        vo.setProviderId(provider.getProviderId());
        vo.setProviderName(provider.getProviderName());
        vo.setLogoSlug(provider.getLogoSlug());
        vo.setModelPrefixes(provider.getModelPrefixes());
        vo.setStatus(provider.getStatus());
        vo.setSortOrder(provider.getSortOrder());
        vo.setRemark(provider.getRemark());
        vo.setCreateTime(provider.getCreateTime());
        return vo;
    }

    private int safeSortOrder(LlmProvider provider) {
        return provider.getSortOrder() == null ? Integer.MAX_VALUE : provider.getSortOrder();
    }

    private record ProviderPrefixMatch(LlmProvider provider, int prefixLength) {
    }
}
