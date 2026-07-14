package org.afo.system.controller.gateway;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.domain.R;
import org.afo.llm.domain.LlmAppClient;
import org.afo.llm.domain.LlmApiKey;
import org.afo.llm.mapper.LlmAppClientMapper;
import org.afo.llm.mapper.LlmApiKeyMapper;
import org.afo.system.domain.SysUser;
import org.afo.system.domain.vo.GatewayApiKeyVo;
import org.afo.system.mapper.SysUserMapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 网关 API Key 控制器
 *
 * 融合改造：查询 wangbo 的 afo_llm_api_key 表
 *
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway/api-keys")
@RequiredArgsConstructor
@Validated
public class GatewayApiKeyController {

    private final LlmApiKeyMapper llmApiKeyMapper;
    private final LlmAppClientMapper llmAppClientMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 验证 API Key
     * GET /api/gateway/api-keys/validate?key=xxx
     */
    @GetMapping("/validate")
    public R<GatewayApiKeyVo> validateApiKey(@RequestParam("key") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return R.fail("API Key 不能为空");
        }

        // 1. 计算 SHA-256 哈希
        String keyHash = DigestUtil.sha256Hex(apiKey);
        log.debug("Validating API Key with hash: {}...", keyHash.substring(0, 16));

        // 2. 查询 afo_llm_api_key 表
        LlmApiKey entity = llmApiKeyMapper.selectOne(
            new LambdaQueryWrapper<LlmApiKey>()
                .eq(LlmApiKey::getKeyHash, keyHash)
                .eq(LlmApiKey::getDelFlag, "0")
        );

        if (entity == null) {
            log.warn("API Key not found (hash={}...)", keyHash.substring(0, 16));
            return R.fail("API Key 不存在或已失效");
        }

        // 3. 校验状态：仅 "0=正常" 放行
        if (!"0".equals(entity.getStatus())) {
            log.warn("API Key disabled or expired, status={}", entity.getStatus());
            if ("2".equals(entity.getStatus())) {
                return R.fail("API Key 已过期");
            }
            return R.fail("API Key 已停用");
        }

        // 4. 检查过期时间
        if (entity.getExpireTime() != null && entity.getExpireTime().before(new Date())) {
            log.warn("API Key expired at {}", entity.getExpireTime());
            return R.fail("API Key 已过期");
        }

        // 5. 构建响应 VO
        GatewayApiKeyVo vo = new GatewayApiKeyVo();
        vo.setTenantId(entity.getTenantId());
        vo.setApiKeyId(String.valueOf(entity.getKeyId()));
        vo.setKeyMasked(entity.getKeyPrefix() + "****...");
        vo.setStatus(entity.getStatus());
        vo.setKeyScope(entity.getKeyScope());
        vo.setClientId(entity.getClientId());
        if (entity.getClientId() != null) {
            LlmAppClient appClient = llmAppClientMapper.selectById(entity.getClientId());
            if (appClient != null) {
                vo.setProjectId(appClient.getProjectId());
            }
        }
        vo.setOwnerUserId(entity.getOwnerUserId());
        if (entity.getOwnerUserId() != null) {
            SysUser owner = sysUserMapper.selectById(entity.getOwnerUserId());
            if (owner != null) {
                vo.setDeptId(owner.getDeptId());
            }
        }

        // 6. 异步更新最后使用时间（不阻塞响应）
        try {
            entity.setLastUsedTime(new Date());
            llmApiKeyMapper.updateById(entity);
        } catch (Exception e) {
            log.warn("Failed to update last_used_time: {}", e.getMessage());
        }

        return R.ok(vo);
    }
}
