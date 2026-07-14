package org.afo.strategy.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.strategy.domain.RoutingConfigRule;
import org.afo.strategy.domain.bo.RoutingConfigSimulateBo;
import org.afo.strategy.domain.vo.RoutingConfigRuleVo;
import org.afo.strategy.domain.vo.RoutingConfigSimulationVo;
import org.afo.strategy.domain.vo.RoutingConfigStatsVo;

import java.util.List;

/**
 * 路由配置规则服务接口。
 *
 * @author AI-FinOps Team
 * @since 2026-05-18
 */
public interface IRoutingConfigRuleService {

    TableDataInfo<RoutingConfigRuleVo> queryPageList(RoutingConfigRule rule, PageQuery pageQuery);

    RoutingConfigRuleVo queryById(Long ruleId);

    void insert(RoutingConfigRule rule);

    void update(RoutingConfigRule rule);

    void updateStatus(Long ruleId, String status);

    void deleteByIds(List<Long> ruleIds);

    void copy(Long ruleId);

    List<RoutingConfigRuleVo> getActiveRules(String tenantId);

    RoutingConfigStatsVo getStats(String tenantId);

    RoutingConfigSimulationVo simulate(RoutingConfigSimulateBo request);
}
