declare namespace Api {
  namespace Gateway {
    interface ApiKeySearchParams {
      tenantId?: string;
      keyMasked?: string;
      status?: string;
      teamTag?: string;
    }

    interface ApiKeyList {
      rows: ApiKeyItem[];
      total: number;
    }

    interface ApiKeyItem {
      id: CommonType.IdType;
      tenantId: string;
      keyMasked: string;
      keyPrefix: string;
      teamTag: string | null;
      status: string;
      expireTime: string | null;
      lastUsedTime: string | null;
      createTime: string;
    }

    interface ApiKeyOperateParams {
      id?: CommonType.IdType;
      tenantId: string;
      teamTag?: string;
      status?: string;
      expireTime?: string;
      remark?: string;
    }

    type WhitelistSearchParams = CommonType.RecordNullable<
      Pick<WhitelistItem, 'tenantId' | 'policyName' | 'defaultMode' | 'status'> & Api.Common.CommonSearchParams
    >;

    type WhitelistList = Api.Common.PaginatingQueryRecord<WhitelistItem>;

    interface ModelAccessModelDetail {
      modelCode: string;
      displayName: string;
      provider: string | null;
      modelType: string | null;
      status: string | null;
    }

    interface WhitelistItem {
      policyId: CommonType.IdType;
      tenantId: string;
      policyName: string;
      defaultMode: string;
      allowedModels: string;
      deniedModels: string;
      allowedModelCount: number;
      deniedModelCount: number;
      allowedModelDetails: ModelAccessModelDetail[];
      deniedModelDetails: ModelAccessModelDetail[];
      status: string;
      effectiveStart: string | null;
      effectiveEnd: string | null;
      remark: string;
      createTime: string;
      updateTime: string;
      updateBy: CommonType.IdType | null;
    }

    interface WhitelistOperateParams {
      policyId?: CommonType.IdType;
      tenantId: string;
      policyName: string;
      defaultMode: string;
      allowedModels: string[];
      deniedModels: string[];
      effectiveStart?: string | null;
      effectiveEnd?: string | null;
      remark?: string;
      status?: string;
    }

    type RoutingActionType = 'ORIGINAL_MODEL' | 'TARGET_MODEL' | 'MODEL_GROUP' | 'CLASSIFIER' | 'DENY';
    type RoutingExecutionMode = 'ENFORCE' | 'RECORD_ONLY';
    type RoutingPathMatchType = 'EXACT' | 'PREFIX' | 'REGEX';
    type RoutingModelType = 'chat' | 'embedding' | 'image' | 'audio' | 'video';
    type RoutingClassifierResult = 'SIMPLE' | 'COMPLEX' | 'UNKNOWN';
    type RoutingConditionLogic = 'ALL' | 'ANY';

    interface RoutingMatchConfig {
      logic?: RoutingConditionLogic;
      apiKeyId?: string | null;
      apiKeyIds?: string[];
      teamTag?: string | null;
      departments?: string[];
      userIds?: string[];
      appIds?: string[];
      path?: string | null;
      pathMatchType?: RoutingPathMatchType | null;
      originalModels?: string[];
      modelTypes?: RoutingModelType[];
      keywords?: string[];
      keywordLogic?: RoutingConditionLogic;
      toolNames?: string[];
      toolLogic?: RoutingConditionLogic;
      headers?: Record<string, string>;
    }

    interface RoutingActionConfig {
      actionType: RoutingActionType;
      targetModel?: string | null;
      targetModelGroup?: string | null;
      simpleModel?: string | null;
      complexModel?: string | null;
      denyReason?: string | null;
    }

    interface RoutingFallbackConfig {
      fallbackMode?: 'ORIGINAL_MODEL' | 'TARGET_MODEL' | 'MODEL_GROUP' | 'DENY';
      fallbackModels?: string[];
      defaultAction?: RoutingActionType;
    }

    interface RoutingConfigRule {
      ruleId: CommonType.IdType;
      tenantId: string;
      ruleName: string;
      priority: number;
      matchConfig: RoutingMatchConfig;
      actionType: RoutingActionType;
      actionConfig: RoutingActionConfig;
      fallbackConfig: RoutingFallbackConfig;
      executionMode: RoutingExecutionMode;
      status: string;
      rolloutPercent?: number | null;
      effectiveStart?: string | null;
      effectiveEnd?: string | null;
      hitCount?: number | null;
      lastHitTime?: string | null;
      remark?: string | null;
      createTime?: string | null;
      createBy?: CommonType.IdType | null;
      updateTime?: string | null;
      updateBy?: CommonType.IdType | null;
    }

    type RoutingConfigSearchParams = CommonType.RecordNullable<
      Pick<RoutingConfigRule, 'tenantId' | 'ruleName' | 'executionMode' | 'status'> &
        Partial<Pick<RoutingActionConfig, 'actionType'>> &
        Api.Common.CommonSearchParams
    >;

    type RoutingConfigList = Api.Common.PaginatingQueryRecord<RoutingConfigRule>;

    interface RoutingConfigStats {
      totalRules: number;
      todayNewRules: number;
      enabledRules: number;
      enabledRate: number;
      recordOnlyRules: number;
      todayHitCount: number;
      todayHitGrowthRate: number;
      fallbackHitCount: number;
      denyHitCount: number;
    }

    interface RoutingConfigOperateParams {
      ruleId?: CommonType.IdType;
      tenantId: string;
      ruleName: string;
      priority: number;
      matchConfig: RoutingMatchConfig;
      actionConfig: RoutingActionConfig;
      fallbackConfig: RoutingFallbackConfig;
      executionMode: RoutingExecutionMode;
      status: string;
      rolloutPercent?: number | null;
      effectiveStart?: string | null;
      effectiveEnd?: string | null;
      remark?: string | null;
    }

    interface RoutingSimulateRequest {
      tenantId?: string | null;
      apiKeyId?: string | null;
      apiKeyIds?: string[];
      department?: string | null;
      departments?: string[];
      userId?: string | null;
      userIds?: string[];
      appId?: string | null;
      appIds?: string[];
      teamTag?: string | null;
      path?: string | null;
      model?: string | null;
      models?: string[];
      modelType?: RoutingModelType | null;
      prompt?: string | null;
      toolNames?: string[];
      headers?: Record<string, string>;
    }

    interface RoutingSimulateResult {
      matched: boolean;
      ruleId?: CommonType.IdType | null;
      ruleName?: string | null;
      actionType?: RoutingActionType | null;
      executionMode?: RoutingExecutionMode | null;
      sourceModel?: string | null;
      targetModel?: string | null;
      targetModelGroup?: string | null;
      classificationResult?: RoutingClassifierResult | null;
      fallbackModels?: string[];
      fallbackModel?: string | null;
      fallbackReason?: string | null;
      matchSummary?: string | null;
      reason?: string | null;
      recordOnly?: boolean;
      decisionLatencyMs?: number | null;
    }

    type RoutingLogSearchParams = CommonType.RecordNullable<
      Pick<
        RoutingLogItem,
        | 'tenantId'
        | 'requestId'
        | 'routeReason'
        | 'denyLayer'
        | 'model'
        | 'targetModel'
        | 'fallbackModel'
        | 'ruleName'
        | 'actionType'
        | 'executionMode'
        | 'createTime'
        | 'ruleId'
        | 'sourceModel'
      > &
        Api.Common.CommonSearchParams
    >;

    type RoutingLogList = Api.Common.PaginatingQueryRecord<RoutingLogItem>;

    interface RoutingLogItem {
      id: CommonType.IdType;
      tenantId: string;
      requestId: string;
      model: string;
      sourceModel?: string | null;
      targetModel: string;
      routeReason: string;
      whitelistHit: boolean;
      routingRuleHit?: boolean;
      ruleName?: string | null;
      actionType?: RoutingActionType | null;
      executionMode?: RoutingExecutionMode | null;
      matchSummary?: string | null;
      fallbackApplied?: boolean | null;
      fallbackModel?: string | null;
      fallbackReason?: string | null;
      classificationResult?: RoutingClassifierResult | null;
      classifierConfidence?: number | null;
      teamTag?: string | null;
      path?: string | null;
      decisionLatencyMs: number;
      denyReason: string;
      policyId: CommonType.IdType | null;
      ruleId?: CommonType.IdType | null;
      denyLayer: string | null;
      createTime: string;
    }

    type RoutingConfigChangeLogSearchParams = CommonType.RecordNullable<
      Pick<RoutingConfigChangeLogItem, 'tenantId' | 'ruleId' | 'operatorName' | 'operationType'> &
        Api.Common.CommonSearchParams & {
          keyword?: string | null;
        }
    >;

    type RoutingConfigChangeLogList = Api.Common.PaginatingQueryRecord<RoutingConfigChangeLogItem>;

    interface RoutingConfigChangeLogItem {
      logId: CommonType.IdType;
      tenantId: string;
      ruleId: CommonType.IdType;
      ruleName?: string | null;
      operatorName?: string | null;
      operationType: string;
      changeContent?: string | null;
      beforeContent?: string | null;
      afterContent?: string | null;
      changeReason?: string | null;
      effectiveStart?: string | null;
      effectiveEnd?: string | null;
      runtimeStart?: string | null;
      runtimeEnd?: string | null;
      createTime?: string | null;
      updateTime?: string | null;
    }

    interface RoutingLogStats {
      todayHitCount: number;
      todayHitGrowthRate: number;
      fallbackHitCount: number;
      denyHitCount: number;
    }

    /** T10b — 评分分析 */

    interface ScoringTierDistribution {
      tier: string;
      count: number;
      percentage: number;
      avgConfidence: number;
    }

    interface ScoringCategoryDistribution {
      category: string;
      count: number;
      percentage: number;
      avgConfidence: number;
    }

    interface ScoringConfidenceBracket {
      bracket: string;
      min: number;
      max: number;
      count: number;
      percentage: number;
    }

    interface ScoringLatencyBracket {
      bracket: string;
      minMs: number;
      maxMs: number | null;
      count: number;
      percentage: number;
    }

    interface ScoringReasonDistribution {
      reason: string;
      count: number;
      percentage: number;
    }

    /** 白名单离线挖掘推荐 */
    interface WhitelistRecommendation {
      /** 推荐 ID */
      id: CommonType.IdType;
      /** 租户 ID */
      tenantId: string;
      /** 挖掘来源：KEYWORD */
      miningSource: string;
      /** 来源请求路径 */
      requestPath: string;
      /** 关键词摘要（如 "法律/合同/审阅等"） */
      keyword: string;
      /** 推荐的正则表达式 */
      recommendedPattern: string;
      /** 匹配类型 */
      matchType: 'EXACT' | 'PREFIX' | 'REGEX';
      /** 7 天内命中次数 */
      requestCount: number;
      /** 平均置信度 */
      avgConfidence: number;
      /** 主要请求模型 */
      dominantModel: string | null;
      /** 推荐理由文案 */
      reason: string;
      /** 推荐状态 */
      status: 'pending' | 'accepted' | 'rejected' | 'expired';
      /** 用户接受后关联的 afo_whitelist_rules.id */
      acceptedRuleId: CommonType.IdType | null;
      /** 过期时间 */
      expiredAt: string;
      /** 备注 */
      remark: string | null;
    }

    /** 接受推荐返回 */
    interface WhitelistRecommendAcceptResult {
      /** 生成的规则 ID */
      ruleId: CommonType.IdType;
      /** 是否试运行模式 */
      dryRun: boolean;
    }
  }
}
