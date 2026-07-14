package org.afo.strategy.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.common.idempotent.annotation.RepeatSubmit;
import org.afo.common.log.annotation.Log;
import org.afo.common.log.enums.BusinessType;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.tenant.helper.TenantHelper;
import org.afo.strategy.domain.RoutingConfigRule;
import org.afo.strategy.domain.bo.RoutingConfigSimulateBo;
import org.afo.strategy.domain.vo.RoutingConfigRuleVo;
import org.afo.strategy.domain.vo.RoutingConfigSimulationVo;
import org.afo.strategy.domain.vo.RoutingConfigStatsVo;
import org.afo.strategy.service.IRoutingConfigRuleService;
import org.afo.strategy.support.GatewayStrategyCachePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 路由配置规则控制器。
 *
 * @author AI-FinOps Team
 * @since 2026-05-18
 */
@RestController
@RequestMapping("/api/gateway/routing-config")
@RequiredArgsConstructor
@Validated
public class RoutingConfigRuleController {

    private final IRoutingConfigRuleService routingConfigRuleService;
    private final GatewayStrategyCachePublisher cachePublisher;

    @Value("${afo.gateway.admin.internal-token:}")
    private String internalToken;

    /** 查询路由配置列表。 */
    @SaCheckPermission("gateway:whitelist:list")
    @GetMapping("/list")
    public TableDataInfo<RoutingConfigRuleVo> list(RoutingConfigRule rule, PageQuery pageQuery) {
        return routingConfigRuleService.queryPageList(rule, pageQuery);
    }

    /** 查询路由配置统计。 */
    @SaCheckPermission("gateway:whitelist:list")
    @GetMapping("/stats")
    public R<RoutingConfigStatsVo> stats(RoutingConfigRule rule) {
        return R.ok(routingConfigRuleService.getStats(rule.getTenantId()));
    }

    /** 查询路由配置详情。 */
    @SaCheckPermission("gateway:whitelist:list")
    @GetMapping("/{ruleId}")
    public R<RoutingConfigRuleVo> getInfo(@PathVariable Long ruleId) {
        return R.ok(routingConfigRuleService.queryById(ruleId));
    }

    /** 新增路由配置。 */
    @SaCheckPermission("gateway:whitelist:add")
    @Log(title = "路由配置", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody RoutingConfigRule rule) {
        routingConfigRuleService.insert(rule);
        cachePublisher.publishRoutingConfig(rule.getTenantId());
        return R.ok();
    }

    /** 修改路由配置。 */
    @SaCheckPermission("gateway:whitelist:edit")
    @Log(title = "路由配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RoutingConfigRule rule) {
        routingConfigRuleService.update(rule);
        cachePublisher.publishRoutingConfig(rule.getTenantId());
        return R.ok();
    }

    /** 修改路由配置状态。 */
    @SaCheckPermission("gateway:whitelist:edit")
    @Log(title = "路由配置", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public R<Void> changeStatus(@RequestBody RoutingConfigRule rule) {
        RoutingConfigRuleVo existing = routingConfigRuleService.queryById(rule.getRuleId());
        routingConfigRuleService.updateStatus(rule.getRuleId(), rule.getStatus());
        cachePublisher.publishRoutingConfig(existing != null ? existing.getTenantId() : TenantHelper.getTenantId());
        return R.ok();
    }

    /** 删除路由配置。 */
    @SaCheckPermission("gateway:whitelist:remove")
    @Log(title = "路由配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ruleIds) {
        routingConfigRuleService.deleteByIds(List.of(ruleIds));
        cachePublisher.publishRoutingConfig(TenantHelper.getTenantId());
        return R.ok();
    }

    /** 复制路由配置。 */
    @SaCheckPermission("gateway:whitelist:add")
    @Log(title = "路由配置复制", businessType = BusinessType.INSERT)
    @PostMapping("/{ruleId}/copy")
    public R<Void> copy(@PathVariable Long ruleId) {
        RoutingConfigRuleVo existing = routingConfigRuleService.queryById(ruleId);
        routingConfigRuleService.copy(ruleId);
        cachePublisher.publishRoutingConfig(existing != null ? existing.getTenantId() : TenantHelper.getTenantId());
        return R.ok();
    }

    /** 手动刷新网关路由配置缓存。 */
    @SaCheckPermission("gateway:whitelist:edit")
    @Log(title = "路由配置缓存刷新", businessType = BusinessType.UPDATE)
    @PostMapping("/cache/refresh")
    public R<Void> refreshCache() {
        cachePublisher.publishRoutingConfig(TenantHelper.getTenantId());
        return R.ok();
    }

    /** 查询租户当前启用且有效的路由配置，供网关读取。 */
    @GetMapping("/{tenantId}/active")
    public R<List<RoutingConfigRuleVo>> active(@PathVariable String tenantId,
                                               @RequestHeader(value = "X-Internal-Token", required = false) String token,
                                               HttpServletResponse response) {
        if (internalToken != null && !internalToken.isBlank() && !internalToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return R.fail(401, "Unauthorized");
        }
        return R.ok(routingConfigRuleService.getActiveRules(tenantId));
    }

    /** 控制面路由模拟，不写日志、不增加命中次数。 */
    @SaCheckPermission("gateway:whitelist:list")
    @PostMapping("/simulate")
    public R<RoutingConfigSimulationVo> simulate(@RequestBody RoutingConfigSimulateBo request) {
        return R.ok(routingConfigRuleService.simulate(request));
    }
}
