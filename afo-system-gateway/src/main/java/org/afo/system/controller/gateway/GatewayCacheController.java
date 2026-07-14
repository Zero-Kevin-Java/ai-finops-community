package org.afo.system.controller.gateway;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.common.log.annotation.Log;
import org.afo.common.log.enums.BusinessType;
import org.afo.system.service.impl.GatewayCachePublisher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 网关缓存管理控制器
 * 
 * 提供缓存刷新接口，供前端手动触发或通过 Redis Pub/Sub 通知网关清除旧缓存。
 * 
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/api/gateway/cache")
@RequiredArgsConstructor
@Validated
public class GatewayCacheController {

    private final GatewayCachePublisher gatewayCachePublisher;

    /**
     * 刷新网关缓存
     * @param type 缓存类型: apikey / whitelist / model-access / routing-config / cache-config
     */
    @SaCheckPermission("gateway:cache:refresh")
    @Log(title = "网关缓存刷新", businessType = BusinessType.UPDATE)
    @PostMapping("/refresh")
    public R<Void> refreshCache(@RequestParam String type) {
        gatewayCachePublisher.publishRefresh(type, "*");
        return R.ok();
    }
}
