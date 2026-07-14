package org.afo.strategy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.strategy.domain.RoutingConfigRule;
import org.afo.strategy.domain.bo.RoutingConfigSimulateBo;
import org.afo.strategy.domain.vo.RoutingConfigRuleVo;
import org.afo.strategy.domain.vo.RoutingConfigSimulationVo;
import org.afo.strategy.domain.vo.RoutingConfigStatsVo;
import org.afo.strategy.mapper.RoutingConfigRuleMapper;
import org.afo.strategy.service.IRoutingConfigRuleService;
import org.afo.strategy.support.ModelCatalogLookup;
import org.afo.strategy.support.PolicyJsonCodec;
import org.afo.strategy.support.StrategyTenantResolver;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 路由配置规则服务实现。
 *
 * <p>该服务只负责控制面配置、校验和模拟，不执行 API Key 授权或模型运行策略限制。</p>
 *
 * @author AI-FinOps Team
 * @since 2026-05-18
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RoutingConfigRuleServiceImpl implements IRoutingConfigRuleService {

    private static final String STATUS_ENABLED = "0";
    private static final String STATUS_DISABLED = "1";
    private static final String MODE_ENFORCE = "ENFORCE";
    private static final Set<String> ACTION_TYPES = Set.of(
        "ORIGINAL_MODEL", "TARGET_MODEL");
    private static final Set<String> PATH_MATCH_TYPES = Set.of("EXACT", "EQUALS", "PREFIX", "REGEX");
    private static final Set<String> TEXT_MATCH_TYPES = Set.of("EXACT", "EQUALS", "PREFIX", "CONTAINS", "REGEX");
    private static final Set<String> KEYWORD_FIELDS = Set.of("prompt", "messages", "input");
    private static final Set<String> CONDITION_LOGICS = Set.of("ALL", "ANY");

    private final RoutingConfigRuleMapper routingConfigRuleMapper;
    private final ModelCatalogLookup modelCatalogLookup;
    private final StrategyTenantResolver tenantResolver;
    private final PolicyJsonCodec policyJsonCodec;

    @Override
    public TableDataInfo<RoutingConfigRuleVo> queryPageList(RoutingConfigRule rule, PageQuery pageQuery) {
        LambdaQueryWrapper<RoutingConfigRule> lqw = buildQueryWrapper(rule);
        Page<RoutingConfigRuleVo> result = routingConfigRuleMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public RoutingConfigRuleVo queryById(Long ruleId) {
        return routingConfigRuleMapper.selectVoById(ruleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(RoutingConfigRule rule) {
        fillDefaults(rule);
        validateForSave(rule);
        routingConfigRuleMapper.insert(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoutingConfigRule rule) {
        if (rule.getRuleId() == null) {
            throw new ServiceException("规则 ID 不能为空");
        }
        RoutingConfigRule existing = routingConfigRuleMapper.selectById(rule.getRuleId());
        if (existing == null) {
            throw new ServiceException("路由配置规则不存在");
        }
        if (StringUtils.isBlank(rule.getTenantId())) {
            rule.setTenantId(existing.getTenantId());
        }
        fillDefaults(rule);
        validateForSave(rule);
        routingConfigRuleMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long ruleId, String status) {
        if (ruleId == null) {
            throw new ServiceException("规则 ID 不能为空");
        }
        RoutingConfigRule rule = routingConfigRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new ServiceException("路由配置规则不存在");
        }
        RoutingConfigRule before = copyRule(rule);
        rule.setStatus(status);
        validateForSave(rule);
        routingConfigRuleMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return;
        }
        List<RoutingConfigRule> existingRules = routingConfigRuleMapper.selectBatchIds(ruleIds);
        routingConfigRuleMapper.deleteBatchIds(ruleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copy(Long ruleId) {
        RoutingConfigRule existing = routingConfigRuleMapper.selectById(ruleId);
        if (existing == null) {
            throw new ServiceException("路由配置规则不存在");
        }
        RoutingConfigRule copied = copyRule(existing);
        copied.setRuleId(null);
        copied.setRuleName(existing.getRuleName() + " - 副本");
        copied.setHitCount(0L);
        copied.setLastHitTime(null);
        copied.setCreateTime(null);
        copied.setUpdateTime(null);
        copied.setCreateBy(null);
        copied.setUpdateBy(null);
        validateForSave(copied);
        routingConfigRuleMapper.insert(copied);
    }

    @Override
    public List<RoutingConfigRuleVo> getActiveRules(String tenantId) {
        String resolvedTenantId = tenantResolver.resolve(tenantId);
        Date now = new Date();
        return routingConfigRuleMapper.selectVoList(
            Wrappers.lambdaQuery(RoutingConfigRule.class)
                .eq(RoutingConfigRule::getTenantId, resolvedTenantId)
                .eq(RoutingConfigRule::getStatus, STATUS_ENABLED)
                .eq(RoutingConfigRule::getDelFlag, "0")
                .and(wrapper -> wrapper.isNull(RoutingConfigRule::getEffectiveStart)
                    .or()
                    .le(RoutingConfigRule::getEffectiveStart, now))
                .and(wrapper -> wrapper.isNull(RoutingConfigRule::getEffectiveEnd)
                    .or()
                    .ge(RoutingConfigRule::getEffectiveEnd, now))
                .orderByAsc(RoutingConfigRule::getPriority)
                .orderByDesc(RoutingConfigRule::getUpdateTime)
                .orderByDesc(RoutingConfigRule::getCreateTime));
    }

    @Override
    public RoutingConfigStatsVo getStats(String tenantId) {
        String resolvedTenantId = tenantResolver.resolve(tenantId);
        Date todayStart = startOfDay(0);
        Date tomorrowStart = startOfDay(1);
        Date yesterdayStart = startOfDay(-1);

        long totalRules = routingConfigRuleMapper.selectCount(baseStatsWrapper(resolvedTenantId));
        long enabledRules = routingConfigRuleMapper.selectCount(
            baseStatsWrapper(resolvedTenantId).eq(RoutingConfigRule::getStatus, STATUS_ENABLED));
        long todayNewRules = routingConfigRuleMapper.selectCount(
            baseStatsWrapper(resolvedTenantId)
                .ge(RoutingConfigRule::getCreateTime, todayStart)
                .lt(RoutingConfigRule::getCreateTime, tomorrowStart));

        List<RoutingConfigRule> todayHitRules = selectHitRules(resolvedTenantId, todayStart, tomorrowStart);
        List<RoutingConfigRule> yesterdayHitRules = selectHitRules(resolvedTenantId, yesterdayStart, todayStart);
        long todayHitCount = sumHitCount(todayHitRules);
        long yesterdayHitCount = sumHitCount(yesterdayHitRules);

        RoutingConfigStatsVo stats = new RoutingConfigStatsVo();
        stats.setTotalRules(totalRules);
        stats.setTodayNewRules(todayNewRules);
        stats.setEnabledRules(enabledRules);
        stats.setEnabledRate(percent(enabledRules, totalRules));
        stats.setTodayHitCount(todayHitCount);
        stats.setTodayHitGrowthRate(growthRate(todayHitCount, yesterdayHitCount));
        stats.setFallbackHitCount(sumHitCount(todayHitRules.stream().filter(this::hasTargetFallback).toList()));
        return stats;
    }

    @Override
    public RoutingConfigSimulationVo simulate(RoutingConfigSimulateBo request) {
        RoutingConfigSimulateBo safeRequest = request == null ? new RoutingConfigSimulateBo() : request;
        for (RoutingConfigRuleVo rule : getActiveRules(safeRequest.getTenantId())) {
            MatchResult matchResult = matches(rule, safeRequest);
            if (matchResult.matched()) {
                return toSimulationResult(rule, safeRequest, matchResult.summary());
            }
        }
        RoutingConfigSimulationVo result = new RoutingConfigSimulationVo();
        result.setMatched(false);
        result.setSourceModel(safeRequest.getSourceModel());
        result.setTargetModel(safeRequest.getSourceModel());
        result.setActionType("ORIGINAL_MODEL");
        result.setExecutionMode(MODE_ENFORCE);
        result.setMatchSummary("未命中路由配置规则");
        return result;
    }

    private LambdaQueryWrapper<RoutingConfigRule> baseStatsWrapper(String tenantId) {
        return Wrappers.lambdaQuery(RoutingConfigRule.class)
            .eq(RoutingConfigRule::getTenantId, tenantId)
            .eq(RoutingConfigRule::getDelFlag, "0");
    }

    private List<RoutingConfigRule> selectHitRules(String tenantId, Date beginTime, Date endTime) {
        return routingConfigRuleMapper.selectList(
            baseStatsWrapper(tenantId)
                .ge(RoutingConfigRule::getLastHitTime, beginTime)
                .lt(RoutingConfigRule::getLastHitTime, endTime));
    }

    private Date startOfDay(int dayOffset) {
        return Date.from(LocalDate.now()
            .plusDays(dayOffset)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant());
    }

    private long sumHitCount(List<RoutingConfigRule> rules) {
        return rules.stream().mapToLong(rule -> rule.getHitCount() == null ? 0L : rule.getHitCount()).sum();
    }

    private boolean hasTargetFallback(RoutingConfigRule rule) {
        String fallbackConfig = rule.getFallbackConfig();
        return StringUtils.isNotBlank(fallbackConfig)
            && (fallbackConfig.contains("\"fallbackModels\"")
                || fallbackConfig.contains("\"fallbackModel\"")
                || fallbackConfig.contains("\"onSourceUnavailable\":\"TARGET_MODEL\"")
                || fallbackConfig.contains("\"onTargetUnavailable\":\"TARGET_MODEL\""));
    }

    private double percent(long numerator, long denominator) {
        if (denominator == 0) {
            return 0D;
        }
        return roundOneDecimal(numerator * 100D / denominator);
    }

    private double growthRate(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100D : 0D;
        }
        return roundOneDecimal((current - previous) * 100D / previous);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10D) / 10D;
    }

    private void validateForSave(RoutingConfigRule rule) {
        if (StringUtils.isBlank(rule.getRuleName())) {
            throw new ServiceException("规则名称不能为空");
        }
        if (rule.getPriority() == null) {
            throw new ServiceException("优先级不能为空");
        }
        if (!ACTION_TYPES.contains(rule.getActionType())) {
            throw new ServiceException("路由动作不合法");
        }
        if (!MODE_ENFORCE.equals(rule.getExecutionMode())) {
            throw new ServiceException("执行模式只能是 ENFORCE");
        }
        if (!STATUS_ENABLED.equals(rule.getStatus()) && !STATUS_DISABLED.equals(rule.getStatus())) {
            throw new ServiceException("状态只能是启用或停用");
        }
        if (rule.getEffectiveStart() != null
            && rule.getEffectiveEnd() != null
            && rule.getEffectiveEnd().before(rule.getEffectiveStart())) {
            throw new ServiceException("生效结束时间不能早于生效开始时间");
        }

        JsonNode matchConfig = policyJsonCodec.parseRequiredObject(rule.getMatchConfig(), "match_config 必须是 JSON 对象");
        JsonNode actionConfig = policyJsonCodec.parseRequiredObject(rule.getActionConfig(), "action_config 必须是 JSON 对象");
        JsonNode fallbackConfig = policyJsonCodec.parseRequiredObject(rule.getFallbackConfig(), "fallback_config 必须是 JSON 对象");

        validateMatchConfig(matchConfig);
        validateActionConfig(rule.getActionType(), actionConfig);
        validateFallbackConfig(fallbackConfig);
    }

    private void validateActionConfig(String actionType, JsonNode actionConfig) {
        List<String> modelCodes = new ArrayList<>();
        if ("TARGET_MODEL".equals(actionType)) {
            String targetModel = requiredText(actionConfig, "targetModel", "TARGET_MODEL 必须配置 targetModel");
            modelCodes.add(targetModel);
        }
        modelCatalogLookup.requireModelCodesExist(modelCodes);
    }

    private void validateMatchConfig(JsonNode matchConfig) {
        validateLogic(matchConfig, "logic");
        validateLogic(matchConfig, "matchLogic");
        validateLogic(matchConfig, "keywordLogic");
        validateLogic(matchConfig, "toolLogic");
        validatePathMatchers(matchConfig.get("paths"));
        validateKeywordMatchers(matchConfig.get("keywords"));
        validateHeaderMatchers(matchConfig.get("headers"));
    }

    private void validateLogic(JsonNode matchConfig, String field) {
        String logic = text(matchConfig, field);
        if (StringUtils.isNotBlank(logic) && !CONDITION_LOGICS.contains(logic.toUpperCase(Locale.ROOT))) {
            throw new ServiceException(field + " 只能是 ALL 或 ANY");
        }
    }

    private void validatePathMatchers(JsonNode paths) {
        if (paths == null || paths.isNull()) {
            return;
        }
        if (!paths.isArray()) {
            throw new ServiceException("paths 必须是 JSON 数组");
        }
        for (JsonNode path : paths) {
            String type = normalizedRequiredText(path, "type", "path 匹配类型不能为空");
            String value = requiredText(path, "value", "path 匹配值不能为空");
            if (!PATH_MATCH_TYPES.contains(type)) {
                throw new ServiceException("path 匹配类型不合法");
            }
            compileRegexIfNeeded(type, value);
        }
    }

    private void validateKeywordMatchers(JsonNode keywords) {
        if (keywords == null || keywords.isNull()) {
            return;
        }
        if (!keywords.isArray()) {
            throw new ServiceException("keywords 必须是 JSON 数组");
        }
        for (JsonNode keyword : keywords) {
            String field = requiredText(keyword, "field", "关键词字段不能为空");
            if (!KEYWORD_FIELDS.contains(field)) {
                throw new ServiceException("关键词字段只能是 prompt、messages 或 input");
            }
            String type = normalizedRequiredText(keyword, "type", "关键词匹配类型不能为空");
            String value = requiredText(keyword, "value", "关键词匹配值不能为空");
            if (!TEXT_MATCH_TYPES.contains(type)) {
                throw new ServiceException("关键词匹配类型不合法");
            }
            compileRegexIfNeeded(type, value);
        }
    }

    private void validateHeaderMatchers(JsonNode headers) {
        if (headers == null || headers.isNull()) {
            return;
        }
        if (!headers.isArray()) {
            throw new ServiceException("headers 必须是 JSON 数组");
        }
        for (JsonNode header : headers) {
            requiredText(header, "name", "Header 名称不能为空");
            String type = normalizedRequiredText(header, "type", "Header 匹配类型不能为空");
            String value = requiredText(header, "value", "Header 匹配值不能为空");
            if (!"EQUALS".equals(type)) {
                throw new ServiceException("Header 匹配类型暂只支持 EQUALS");
            }
        }
    }

    private void validateFallbackConfig(JsonNode fallbackConfig) {
        List<String> fallbackModels = orderedStringList(fallbackConfig.get("fallbackModels"));
        String fallbackModel = text(fallbackConfig, "fallbackModel");
        if (hasInvalidStringArrayItem(fallbackConfig.get("fallbackModels"))) {
            throw new ServiceException("fallbackModels 不能包含空模型");
        }
        addIfPresent(fallbackModels, fallbackModel);
        modelCatalogLookup.requireModelCodesExist(fallbackModels);
    }

    private MatchResult matches(RoutingConfigRuleVo rule, RoutingConfigSimulateBo request) {
        JsonNode matchConfig = policyJsonCodec.parseLenientObject(rule.getMatchConfig());
        List<Boolean> conditionResults = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        if (hasStringList(matchConfig.get("apiKeyIds"))) {
            conditionResults.add(matchesAnyString(matchConfig.get("apiKeyIds"),
                requestValues(request.getApiKeyId(), request.getApiKeyIds()), "apiKeyId", summaries));
        }
        JsonNode departmentConfig = hasStringList(matchConfig.get("departments"))
            ? matchConfig.get("departments")
            : matchConfig.get("teamTags");
        if (hasStringList(departmentConfig)) {
            conditionResults.add(matchesAnyString(departmentConfig,
                requestValues(firstNonBlank(request.getDepartment(), request.getTeamTag()), request.getDepartments()),
                "department", summaries));
        }
        if (hasStringList(matchConfig.get("userIds"))) {
            conditionResults.add(matchesAnyString(matchConfig.get("userIds"),
                requestValues(request.getUserId(), request.getUserIds()), "userId", summaries));
        }
        if (hasStringList(matchConfig.get("appIds"))) {
            conditionResults.add(matchesAnyString(matchConfig.get("appIds"),
                requestValues(request.getAppId(), request.getAppIds()), "appId", summaries));
        }
        if (hasStringList(matchConfig.get("sourceModels"))) {
            conditionResults.add(matchesAnyString(matchConfig.get("sourceModels"),
                requestValues(request.getSourceModel(), request.getSourceModels()), "sourceModel", summaries));
        }
        if (hasStringList(matchConfig.get("modelTypes"))) {
            conditionResults.add(matchesAnyString(matchConfig.get("modelTypes"), request.getModelType(), "modelType", summaries));
        }
        if (hasStringList(matchConfig.get("tools"))) {
            conditionResults.add(matchesTools(matchConfig.get("tools"), request.getTools(),
                normalizedText(matchConfig, "toolLogic", "ANY"), summaries));
        }
        if (hasArray(matchConfig.get("paths"))) {
            conditionResults.add(matchesPaths(matchConfig.get("paths"), request.getPath(), summaries));
        }
        if (hasArray(matchConfig.get("keywords"))) {
            conditionResults.add(matchesKeywords(matchConfig.get("keywords"), request,
                normalizedText(matchConfig, "keywordLogic", "ANY"), summaries));
        }
        if (hasArray(matchConfig.get("headers"))) {
            conditionResults.add(matchesHeaders(matchConfig.get("headers"), request.getHeaders(), summaries));
        }
        if (conditionResults.isEmpty()) {
            return MatchResult.matched("全部请求");
        }
        boolean matched = "ANY".equals(normalizedText(matchConfig, "logic", normalizedText(matchConfig, "matchLogic", "ALL")))
            ? conditionResults.stream().anyMatch(Boolean.TRUE::equals)
            : conditionResults.stream().allMatch(Boolean.TRUE::equals);
        return matched
            ? MatchResult.matched(summaries.isEmpty() ? "全部请求" : String.join("; ", summaries))
            : MatchResult.notMatched();
    }

    private boolean matchesAnyString(JsonNode configured, String actual, String fieldName, List<String> summaries) {
        return matchesAnyString(configured, requestValues(actual, null), fieldName, summaries);
    }

    private boolean matchesAnyString(JsonNode configured, List<String> actualValues, String fieldName, List<String> summaries) {
        List<String> values = orderedStringList(configured);
        if (values.isEmpty()) {
            return true;
        }
        List<String> actuals = actualValues == null ? List.of() : actualValues;
        List<String> matchedValues = actuals.stream().filter(values::contains).toList();
        boolean matched = !matchedValues.isEmpty();
        if (matched) {
            summaries.add(fieldName + "=" + String.join(",", matchedValues));
        }
        return matched;
    }

    private List<String> requestValues(String singleValue, List<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.stream().filter(StringUtils::isNotBlank).forEach(result::add);
        }
        if (StringUtils.isNotBlank(singleValue)) {
            result.add(singleValue);
        }
        return new ArrayList<>(result);
    }

    private boolean matchesTools(JsonNode configured, List<String> actualTools, String logic, List<String> summaries) {
        List<String> values = orderedStringList(configured);
        if (values.isEmpty()) {
            return true;
        }
        if (actualTools == null || actualTools.isEmpty()) {
            return false;
        }
        List<String> matched = values.stream().filter(actualTools::contains).toList();
        boolean conditionMatched = "ALL".equals(logic) ? matched.size() == values.size() : !matched.isEmpty();
        if (conditionMatched) {
            summaries.add("tools=" + String.join(",", matched));
        }
        return conditionMatched;
    }

    private boolean matchesPaths(JsonNode paths, String actualPath, List<String> summaries) {
        if (paths == null || !paths.isArray() || paths.isEmpty()) {
            return true;
        }
        if (StringUtils.isBlank(actualPath)) {
            return false;
        }
        for (JsonNode path : paths) {
            String type = normalizedText(path, "type", "EXACT");
            String value = text(path, "value");
            if (matchesText(type, value, actualPath)) {
                summaries.add("path " + type + " " + value);
                return true;
            }
        }
        return false;
    }

    private boolean matchesKeywords(JsonNode keywords, RoutingConfigSimulateBo request, String logic, List<String> summaries) {
        if (keywords == null || !keywords.isArray() || keywords.isEmpty()) {
            return true;
        }
        int validCount = 0;
        int matchedCount = 0;
        List<String> matchedSummaries = new ArrayList<>();
        for (JsonNode keyword : keywords) {
            String field = text(keyword, "field");
            String type = normalizedText(keyword, "type", "CONTAINS");
            String value = text(keyword, "value");
            String actual = requestText(field, request);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            validCount++;
            if (matchesText(type, value, actual)) {
                matchedSummaries.add("keyword " + field + " " + type);
                matchedCount++;
                if (!"ALL".equals(logic)) {
                    summaries.add("keyword " + field + " " + type);
                    return true;
                }
            }
        }
        boolean conditionMatched = validCount == 0 || matchedCount == validCount;
        if (conditionMatched) {
            summaries.addAll(matchedSummaries);
        }
        return conditionMatched;
    }

    private boolean matchesHeaders(JsonNode headers, Map<String, String> actualHeaders, List<String> summaries) {
        if (headers == null || !headers.isArray() || headers.isEmpty()) {
            return true;
        }
        if (actualHeaders == null || actualHeaders.isEmpty()) {
            return false;
        }
        int validCount = 0;
        int matchedCount = 0;
        List<String> matchedSummaries = new ArrayList<>();
        for (JsonNode header : headers) {
            String name = text(header, "name");
            String type = normalizedText(header, "type", "EQUALS");
            String value = text(header, "value");
            String actual = headerValue(actualHeaders, name);
            if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
                continue;
            }
            validCount++;
            if (matchesText(type, value, actual)) {
                matchedSummaries.add("header " + name + " " + type);
                matchedCount++;
            }
        }
        boolean conditionMatched = validCount == 0 || matchedCount == validCount;
        if (conditionMatched) {
            summaries.addAll(matchedSummaries);
        }
        return conditionMatched;
    }

    private RoutingConfigSimulationVo toSimulationResult(
        RoutingConfigRuleVo rule,
        RoutingConfigSimulateBo request,
        String matchSummary) {
        JsonNode actionConfig = policyJsonCodec.parseLenientObject(rule.getActionConfig());
        JsonNode fallbackConfig = policyJsonCodec.parseLenientObject(rule.getFallbackConfig());
        RoutingConfigSimulationVo result = new RoutingConfigSimulationVo();
        result.setMatched(true);
        result.setRuleId(rule.getRuleId());
        result.setRuleName(rule.getRuleName());
        result.setActionType(rule.getActionType());
        result.setSourceModel(request.getSourceModel());
        result.setExecutionMode(rule.getExecutionMode());
        result.setMatchSummary(matchSummary);
        result.setFallbackModels(orderedStringList(fallbackConfig.get("fallbackModels")));
        result.setFallbackModel(firstFallback(fallbackConfig, result.getFallbackModels()));
        result.setFallbackApplied(false);
        result.setTargetModel(resolveTargetModel(rule.getActionType(), actionConfig, request.getSourceModel()));
        return result;
    }

    private String resolveTargetModel(String actionType, JsonNode actionConfig, String sourceModel) {
        if ("TARGET_MODEL".equals(actionType)) {
            return text(actionConfig, "targetModel");
        }
        return sourceModel;
    }

    private LambdaQueryWrapper<RoutingConfigRule> buildQueryWrapper(RoutingConfigRule rule) {
        RoutingConfigRule query = rule == null ? new RoutingConfigRule() : rule;
        Map<String, Object> params = query.getParams() == null ? Map.of() : query.getParams();
        String matchLogic = paramText(params, "matchLogic");
        String fallbackMode = paramText(params, "fallbackMode");
        Object beginTime = params.get("beginTime");
        Object endTime = params.get("endTime");
        LambdaQueryWrapper<RoutingConfigRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(query.getTenantId()), RoutingConfigRule::getTenantId, query.getTenantId());
        lqw.like(StringUtils.isNotBlank(query.getRuleName()), RoutingConfigRule::getRuleName, query.getRuleName());
        lqw.eq(StringUtils.isNotBlank(query.getActionType()), RoutingConfigRule::getActionType, query.getActionType());
        lqw.eq(StringUtils.isNotBlank(query.getExecutionMode()), RoutingConfigRule::getExecutionMode, query.getExecutionMode());
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), RoutingConfigRule::getStatus, query.getStatus());
        lqw.like(StringUtils.isNotBlank(matchLogic), RoutingConfigRule::getMatchConfig, "\"logic\":\"" + matchLogic + "\"");
        lqw.and(StringUtils.isNotBlank(fallbackMode), wrapper -> wrapper
            .like(RoutingConfigRule::getFallbackConfig, "\"onSourceUnavailable\":\"" + fallbackMode + "\"")
            .or()
            .like(RoutingConfigRule::getFallbackConfig, "\"onTargetUnavailable\":\"" + fallbackMode + "\""));
        lqw.between(beginTime != null && endTime != null, RoutingConfigRule::getEffectiveStart, beginTime, endTime);
        lqw.orderByAsc(RoutingConfigRule::getPriority);
        lqw.orderByDesc(RoutingConfigRule::getUpdateTime);
        lqw.orderByDesc(RoutingConfigRule::getCreateTime);
        return lqw;
    }

    private String statusLabel(String status) {
        return STATUS_ENABLED.equals(status) ? "启用" : "停用";
    }

    private String executionModeLabel(String executionMode) {
        return "生效执行";
    }

    private RoutingConfigRule copyRule(RoutingConfigRule source) {
        RoutingConfigRule target = new RoutingConfigRule();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private String paramText(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private void fillDefaults(RoutingConfigRule rule) {
        rule.setTenantId(tenantResolver.resolve(rule.getTenantId()));
        if (rule.getPriority() == null) {
            rule.setPriority(100);
        }
        if (StringUtils.isBlank(rule.getMatchConfig())) {
            rule.setMatchConfig("{}");
        }
        if (StringUtils.isBlank(rule.getActionConfig())) {
            rule.setActionConfig("{}");
        }
        if (StringUtils.isBlank(rule.getFallbackConfig())) {
            rule.setFallbackConfig("{}");
        }
        if (StringUtils.isBlank(rule.getExecutionMode())) {
            rule.setExecutionMode(MODE_ENFORCE);
        }
        if (StringUtils.isBlank(rule.getStatus())) {
            rule.setStatus(STATUS_ENABLED);
        }
    }

    private RoutingConfigRuleVo toVo(RoutingConfigRule rule) {
        RoutingConfigRuleVo vo = new RoutingConfigRuleVo();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }

    private String requestText(String field, RoutingConfigSimulateBo request) {
        if ("messages".equals(field)) {
            return request.getMessagesText();
        }
        if ("input".equals(field)) {
            return request.getInput();
        }
        return request.getPrompt();
    }

    private String headerValue(Map<String, String> headers, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean matchesText(String type, String expected, String actual) {
        if (StringUtils.isBlank(expected) || actual == null) {
            return false;
        }
        return switch (type) {
            case "PREFIX" -> actual.startsWith(expected);
            case "CONTAINS" -> actual.contains(expected);
            case "REGEX" -> Pattern.compile(expected).matcher(actual).find();
            default -> actual.equals(expected);
        };
    }

    private void compileRegexIfNeeded(String type, String value) {
        if (!"REGEX".equals(type)) {
            return;
        }
        try {
            Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            throw new ServiceException("正则表达式不合法");
        }
    }

    private String requiredText(JsonNode node, String field, String message) {
        String value = text(node, field);
        if (StringUtils.isBlank(value)) {
            throw new ServiceException(message);
        }
        return value;
    }

    private String normalizedRequiredText(JsonNode node, String field, String message) {
        return requiredText(node, field, message).toUpperCase(Locale.ROOT);
    }

    private String normalizedText(JsonNode node, String field, String defaultValue) {
        String value = text(node, field);
        return StringUtils.isBlank(value) ? defaultValue : value.toUpperCase(Locale.ROOT);
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || !node.get(field).isTextual()) {
            return null;
        }
        return node.get(field).asText();
    }

    private boolean hasStringList(JsonNode node) {
        return !orderedStringList(node).isEmpty();
    }

    private boolean hasArray(JsonNode node) {
        return node != null && node.isArray() && !node.isEmpty();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private List<String> orderedStringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual() || StringUtils.isBlank(item.asText())) {
                return List.of();
            }
            values.add(item.asText());
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private boolean hasInvalidStringArrayItem(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (!node.isArray()) {
            return true;
        }
        for (JsonNode item : node) {
            if (!item.isTextual() || StringUtils.isBlank(item.asText())) {
                return true;
            }
        }
        return false;
    }

    private void addIfPresent(List<String> values, String value) {
        if (StringUtils.isNotBlank(value)) {
            values.add(value);
        }
    }

    private String firstFallback(JsonNode fallbackConfig, List<String> fallbackModels) {
        if (!fallbackModels.isEmpty()) {
            return fallbackModels.get(0);
        }
        return text(fallbackConfig, "fallbackModel");
    }

    private record MatchResult(boolean matched, String summary) {

        private static MatchResult matched(String summary) {
            return new MatchResult(true, summary);
        }

        private static MatchResult notMatched() {
            return new MatchResult(false, null);
        }
    }
}
