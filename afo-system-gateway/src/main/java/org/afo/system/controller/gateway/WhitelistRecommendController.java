package org.afo.system.controller.gateway;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.domain.R;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.satoken.utils.LoginHelper;
import org.afo.system.domain.WhitelistRecommendation;
import org.afo.system.mapper.WhitelistRecommendationMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 白名单离线挖掘推荐 API。
 *
 * @author AI-FinOps Team
 * @since 2026-06-15
 */
@Slf4j
@RestController
@RequestMapping("/api/whitelist/recommend")
@RequiredArgsConstructor
public class WhitelistRecommendController {

    private final WhitelistRecommendationMapper recommendationMapper;
    private final JdbcTemplate jdbcTemplate;

    @SaCheckPermission("gateway:whitelist:recommend")
    @GetMapping
    public R<List<WhitelistRecommendation>> list() {
        String tenantId = LoginHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            return R.fail("无法识别当前租户");
        }

        List<WhitelistRecommendation> list = recommendationMapper.selectList(
            new LambdaQueryWrapper<WhitelistRecommendation>()
                .eq(WhitelistRecommendation::getTenantId, tenantId)
                .eq(WhitelistRecommendation::getStatus, "pending")
                .eq(WhitelistRecommendation::getDelFlag, "0")
                .orderByDesc(WhitelistRecommendation::getRequestCount));

        return R.ok(list);
    }

    @SaCheckPermission("gateway:whitelist:recommend")
    @PostMapping("/{id}/accept")
    public R<Map<String, Object>> accept(@PathVariable Long id) {
        String tenantId = LoginHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            return R.fail("无法识别当前租户");
        }

        WhitelistRecommendation rec = recommendationMapper.selectOne(
            new LambdaQueryWrapper<WhitelistRecommendation>()
                .eq(WhitelistRecommendation::getId, id)
                .eq(WhitelistRecommendation::getTenantId, tenantId)
                .eq(WhitelistRecommendation::getStatus, "pending")
                .eq(WhitelistRecommendation::getDelFlag, "0"));

        if (rec == null) {
            return R.fail("推荐记录不存在或已失效");
        }

        long snowflakeId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
        jdbcTemplate.update(
            "INSERT INTO afo_whitelist_rules " +
            "(id, tenant_id, match_type, pattern, remark, enabled, dry_run, hit_count, " +
            " create_dept, create_by, create_time, del_flag) " +
            "VALUES (?, ?, ?, ?, ?, '0', '1', 0, NULL, NULL, NOW(), '0') " +
            "ON CONFLICT DO NOTHING",
            snowflakeId, tenantId,
            rec.getMatchType(), rec.getRecommendedPattern(),
            "离线挖掘推荐: " + rec.getReason());

        rec.setStatus("accepted");
        rec.setAcceptedRuleId(snowflakeId);
        recommendationMapper.updateById(rec);

        log.info("[WhitelistRecommend] Tenant={} accepted recommendation id={}, ruleId={}, pattern={}",
            tenantId, id, snowflakeId, rec.getRecommendedPattern());

        return R.ok("已将规则加入试运行白名单，观察 3 天后可正式启用",
            Map.of("ruleId", snowflakeId, "dryRun", true));
    }

    @SaCheckPermission("gateway:whitelist:recommend")
    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id) {
        String tenantId = LoginHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            return R.fail("无法识别当前租户");
        }

        WhitelistRecommendation rec = recommendationMapper.selectOne(
            new LambdaQueryWrapper<WhitelistRecommendation>()
                .eq(WhitelistRecommendation::getId, id)
                .eq(WhitelistRecommendation::getTenantId, tenantId));

        if (rec != null) {
            rec.setStatus("rejected");
            recommendationMapper.updateById(rec);
            log.info("[WhitelistRecommend] Tenant={} rejected recommendation id={}", tenantId, id);
        }

        return R.ok();
    }
}
