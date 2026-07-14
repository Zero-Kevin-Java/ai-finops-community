package org.afo.system.controller.gateway;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.system.domain.vo.GatewayStatisticsVo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 网关统计控制器
 * 
 * 提供看板统计查询接口，支持按 API Key 或团队聚合。
 * 无 X-Team header 时自动降级为按 API Key 聚合。
 * 
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/api/gateway/statistics")
@RequiredArgsConstructor
public class GatewayStatisticsController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取 Top 5 统计（按 API Key 或团队聚合）
     * 
     * @param teamTag 团队标签（可选，为空时按 API Key 聚合）
     */
    @SaCheckPermission("gateway:statistics:view")
    @GetMapping("/top5")
    public R<List<GatewayStatisticsVo>> getTop5(
            @RequestParam(required = false, value = "teamTag") String teamTag) {
        
        List<GatewayStatisticsVo> result;
        
        if (teamTag != null && !teamTag.isEmpty()) {
            // 按团队聚合查询（需要 afo_route_decision_log 表有 team_tag 字段）
            // L0 阶段此表无 team_tag 字段，降级为按 API Key 聚合
            result = queryByApiKey();
        } else {
            // 无 X-Team 头，按 API Key 聚合
            result = queryByApiKey();
        }
        
        return R.ok(result);
    }

    private List<GatewayStatisticsVo> queryByApiKey() {
        String sql = """
            SELECT 
                COALESCE(ak.key_name, ak.key_prefix, log.api_key_id) AS group_key,
                COUNT(*) AS request_count,
                0.0 AS total_cost
            FROM afo_route_decision_log log
            LEFT JOIN afo_llm_api_key ak
                ON ak.key_id = CAST(log.api_key_id AS BIGINT)
                AND ak.del_flag = '0'
            WHERE log.del_flag = '0'
              AND log.create_time >= NOW() - INTERVAL '7 days'
              AND log.api_key_id IS NOT NULL
              AND log.api_key_id != ''
            GROUP BY group_key
            ORDER BY request_count DESC
            LIMIT 5
            """;
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GatewayStatisticsVo> result = new ArrayList<>();
        
        for (Map<String, Object> row : rows) {
            GatewayStatisticsVo vo = new GatewayStatisticsVo();
            vo.setGroupKey((String) row.get("group_key"));
            vo.setRequestCount(((Number) row.get("request_count")).longValue());
            vo.setTotalCost(((Number) row.get("total_cost")).doubleValue());
            result.add(vo);
        }
        
        // 如果查询结果为空，返回空列表（前端不应显示 Empty，而是显示"暂无数据"引导）
        return result;
    }
}
