package org.afo.strategy.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.common.excel.utils.ExcelUtil;
import org.afo.common.idempotent.annotation.RepeatSubmit;
import org.afo.common.log.annotation.Log;
import org.afo.common.log.enums.BusinessType;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.tenant.helper.TenantHelper;
import org.afo.strategy.domain.ModelAccessPolicy;
import org.afo.strategy.domain.vo.ModelAccessPolicyVo;
import org.afo.strategy.service.IModelAccessPolicyService;
import org.afo.strategy.support.GatewayStrategyCachePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业模型准入管理控制器。
 *
 * <p>新接口使用 /api/gateway/model-access，历史 /api/gateway/whitelist 路径保留一版兼容。</p>
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@RestController
@RequestMapping({"/api/gateway/model-access", "/api/gateway/whitelist"})
@RequiredArgsConstructor
@Validated
public class ModelAccessPolicyController {

    private final IModelAccessPolicyService modelAccessPolicyService;
    private final GatewayStrategyCachePublisher cachePublisher;

    @Value("${afo.gateway.admin.internal-token:}")
    private String internalToken;

    /** 查询企业模型准入配置列表。 */
    @SaCheckPermission("gateway:whitelist:list")
    @GetMapping("/list")
    public TableDataInfo<ModelAccessPolicyVo> list(ModelAccessPolicy policy, PageQuery pageQuery) {
        return modelAccessPolicyService.queryPageList(policy, pageQuery);
    }

    /** 查询企业模型准入配置详情。 */
    @SaCheckPermission("gateway:whitelist:list")
    @GetMapping("/{policyId}")
    public R<ModelAccessPolicyVo> getInfo(@PathVariable Long policyId) {
        return R.ok(modelAccessPolicyService.queryById(policyId));
    }

    /** 查询租户当前有效的企业模型准入配置，供网关读取。 */
    @GetMapping("/{tenantId}/active")
    public R<ModelAccessPolicyVo> getActivePolicy(@PathVariable String tenantId,
                                                   @RequestHeader(value = "X-Internal-Token", required = false) String token,
                                                   HttpServletResponse response) {
        if (internalToken != null && !internalToken.isBlank() && !internalToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return R.fail(401, "Unauthorized");
        }
        return R.ok(modelAccessPolicyService.getActivePolicy(tenantId));
    }

    /** 新增企业模型准入配置。 */
    @SaCheckPermission("gateway:whitelist:add")
    @Log(title = "模型准入管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody ModelAccessPolicy policy) {
        modelAccessPolicyService.insert(policy);
        cachePublisher.publishModelAccess(policy.getTenantId());
        return R.ok();
    }

    /** 修改企业模型准入配置。 */
    @SaCheckPermission("gateway:whitelist:edit")
    @Log(title = "模型准入管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody ModelAccessPolicy policy) {
        modelAccessPolicyService.update(policy);
        cachePublisher.publishModelAccess(policy.getTenantId());
        return R.ok();
    }

    /** 修改企业模型准入配置状态。 */
    @SaCheckPermission("gateway:whitelist:edit")
    @Log(title = "模型准入管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public R<Void> changeStatus(@RequestBody ModelAccessPolicy policy) {
        modelAccessPolicyService.updateStatus(policy.getPolicyId(), policy.getStatus());
        cachePublisher.publishModelAccess(policy.getTenantId());
        return R.ok();
    }

    /** 删除企业模型准入配置。 */
    @SaCheckPermission("gateway:whitelist:remove")
    @Log(title = "模型准入管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{policyIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] policyIds) {
        modelAccessPolicyService.deleteByIds(List.of(policyIds));
        cachePublisher.publishModelAccess(TenantHelper.getTenantId());
        return R.ok();
    }

    /** 导出企业模型准入配置。 */
    @SaCheckPermission("gateway:whitelist:export")
    @Log(title = "模型准入管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ModelAccessPolicy policy, HttpServletResponse response) {
        List<ModelAccessPolicyVo> list = modelAccessPolicyService.queryList(policy);
        ExcelUtil.exportExcel(list, "模型准入管理", ModelAccessPolicyVo.class, response);
    }

    /** 手动刷新网关模型准入缓存。 */
    @SaCheckPermission("gateway:whitelist:edit")
    @Log(title = "模型准入缓存刷新", businessType = BusinessType.UPDATE)
    @PostMapping("/cache/refresh")
    public R<Void> refreshCache() {
        cachePublisher.publishModelAccess(TenantHelper.getTenantId());
        return R.ok();
    }
}
