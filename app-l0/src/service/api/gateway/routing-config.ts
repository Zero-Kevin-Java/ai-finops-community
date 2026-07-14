import { request } from '@/service/request';

const baseUrl = '/api/gateway/routing-config';

/** 获取路由配置列表 */
export async function fetchGetRoutingConfigRuleList(params?: Api.Gateway.RoutingConfigSearchParams) {
  const response = await request<Api.Gateway.RoutingConfigList>({
    url: `${baseUrl}/list`,
    method: 'get',
    params
  });
  if (!response.error && response.data) {
    response.data = normalizeRuleList(response.data);
  }
  return response;
}

/** 获取路由配置详情 */
export async function fetchGetRoutingConfigRuleInfo(ruleId: CommonType.IdType) {
  const response = await request<Api.Gateway.RoutingConfigRule>({
    url: `${baseUrl}/${ruleId}`,
    method: 'get'
  });
  if (!response.error && response.data) {
    response.data = normalizeRule(response.data);
  }
  return response;
}

/** 获取路由配置修改日志 */
export function fetchGetRoutingConfigChangeLogList(
  ruleId: CommonType.IdType,
  params?: Api.Gateway.RoutingConfigChangeLogSearchParams
) {
  return request<Api.Gateway.RoutingConfigChangeLogList>({
    url: `${baseUrl}/${ruleId}/change-logs`,
    method: 'get',
    params
  });
}

/** 获取路由配置统计 */
export function fetchGetRoutingConfigStats(params?: Pick<Api.Gateway.RoutingConfigSearchParams, 'tenantId'>) {
  return request<Api.Gateway.RoutingConfigStats>({
    url: `${baseUrl}/stats`,
    method: 'get',
    params
  });
}

/** 新增路由配置 */
export function fetchCreateRoutingConfigRule(data: Api.Gateway.RoutingConfigOperateParams) {
  return request<boolean>({
    url: baseUrl,
    method: 'post',
    data: serializeRule(data)
  });
}

/** 修改路由配置 */
export function fetchUpdateRoutingConfigRule(data: Api.Gateway.RoutingConfigOperateParams) {
  return request<boolean>({
    url: baseUrl,
    method: 'put',
    data: serializeRule(data)
  });
}

/** 修改路由配置状态 */
export function fetchUpdateRoutingConfigRuleStatus(
  data: Pick<Api.Gateway.RoutingConfigOperateParams, 'ruleId' | 'tenantId' | 'status'>
) {
  return request<boolean>({
    url: `${baseUrl}/status`,
    method: 'put',
    data
  });
}

/** 批量删除路由配置 */
export function fetchBatchDeleteRoutingConfigRules(ids: CommonType.IdType[]) {
  return request<boolean>({
    url: `${baseUrl}/${ids.join(',')}`,
    method: 'delete'
  });
}

/** 复制路由配置 */
export function fetchCopyRoutingConfigRule(ruleId: CommonType.IdType) {
  return request<boolean>({
    url: `${baseUrl}/${ruleId}/copy`,
    method: 'post'
  });
}

/** 模拟路由配置命中结果 */
export function fetchSimulateRoutingConfig(data: Api.Gateway.RoutingSimulateRequest) {
  const models = data.models?.length ? data.models : data.model ? [data.model] : [];
  return request<Api.Gateway.RoutingSimulateResult>({
    url: `${baseUrl}/simulate`,
    method: 'post',
    data: {
      ...data,
      sourceModel: models[0],
      sourceModels: models,
      prompt: data.prompt,
      tools: data.toolNames
    }
  });
}

/** 刷新路由配置网关缓存 */
export function fetchRefreshRoutingConfigCache() {
  return request<boolean>({
    url: `${baseUrl}/cache/refresh`,
    method: 'post'
  });
}

function serializeRule(data: Api.Gateway.RoutingConfigOperateParams) {
  return {
    ...data,
    actionType: data.actionConfig.actionType,
    matchConfig: JSON.stringify(toBackendMatchConfig(data.matchConfig)),
    actionConfig: JSON.stringify(toBackendActionConfig(data.actionConfig)),
    fallbackConfig: JSON.stringify(toBackendFallbackConfig(data.fallbackConfig))
  };
}

function normalizeRuleList(list: Api.Gateway.RoutingConfigList): Api.Gateway.RoutingConfigList {
  return {
    ...list,
    rows: list.rows.map(normalizeRule)
  };
}

function normalizeRule(rule: Api.Gateway.RoutingConfigRule): Api.Gateway.RoutingConfigRule {
  const rawRule = rule as unknown as Record<string, unknown>;
  return {
    ...rule,
    matchConfig: fromBackendMatchConfig(parseObject(rawRule.matchConfig)),
    actionConfig: fromBackendActionConfig(rule.actionType, parseObject(rawRule.actionConfig)),
    fallbackConfig: fromBackendFallbackConfig(parseObject(rawRule.fallbackConfig))
  };
}

function parseObject(value: unknown) {
  if (!value) return {};
  if (typeof value === 'object') return value as Record<string, any>;
  try {
    return JSON.parse(String(value)) as Record<string, any>;
  } catch {
    return {};
  }
}

function toBackendMatchConfig(match: Api.Gateway.RoutingMatchConfig) {
  const apiKeyIds = normalizeStringList(match.apiKeyIds || (match.apiKeyId ? [match.apiKeyId] : []));
  return {
    logic: match.logic || 'ALL',
    apiKeyIds,
    teamTags: normalizeStringList(match.departments || (match.teamTag ? [match.teamTag] : [])),
    departments: normalizeStringList(match.departments),
    userIds: normalizeStringList(match.userIds),
    appIds: normalizeStringList(match.appIds),
    paths: match.path ? [{ type: match.pathMatchType || 'PREFIX', value: match.path }] : [],
    sourceModels: match.originalModels || [],
    keywordLogic: match.keywordLogic || 'ANY',
    keywords: (match.keywords || []).map(value => ({ field: 'prompt', type: 'CONTAINS', value })),
    toolLogic: match.toolLogic || 'ANY',
    tools: match.toolNames || [],
    headers: Object.entries(match.headers || {}).map(([name, value]) => ({ name, type: 'EQUALS', value }))
  };
}

function toBackendActionConfig(action: Api.Gateway.RoutingActionConfig) {
  const modelGroupModels = splitModels(action.targetModelGroup);
  return {
    targetModel: action.targetModel,
    modelGroup: action.targetModelGroup,
    models: modelGroupModels,
    simpleModel: action.simpleModel,
    complexModel: action.complexModel,
    simpleTaskTargetModel: action.simpleModel,
    denyReason: action.denyReason || 'ROUTING_RULE_DENIED'
  };
}

function toBackendFallbackConfig(fallback: Api.Gateway.RoutingFallbackConfig) {
  return {
    fallbackModel: fallback.fallbackModels?.[0],
    fallbackModels: fallback.fallbackModels || [],
    onSourceUnavailable: fallback.fallbackMode,
    onTargetUnavailable: fallback.fallbackMode,
    onClassifierError: fallback.defaultAction,
    maxRetries: 1
  };
}

function fromBackendMatchConfig(match: Record<string, any>): Api.Gateway.RoutingMatchConfig {
  const path = Array.isArray(match.paths) ? match.paths[0] : null;
  const apiKeyIds = normalizeStringList(match.apiKeyIds);
  const departments = normalizeStringList(match.departments || match.teamTags);
  const headers = Array.isArray(match.headers)
    ? Object.fromEntries(match.headers.map((item: any) => [item.name, item.value]).filter(([name]) => Boolean(name)))
    : {};
  return {
    logic: match.logic || match.matchLogic || 'ALL',
    apiKeyId: apiKeyIds[0] || null,
    apiKeyIds,
    teamTag: departments[0] || match.teamTags?.[0] || null,
    departments,
    userIds: normalizeStringList(match.userIds),
    appIds: normalizeStringList(match.appIds),
    path: path?.value || null,
    pathMatchType: path?.type || 'PREFIX',
    originalModels: match.sourceModels || [],
    keywordLogic: match.keywordLogic || 'ANY',
    keywords: Array.isArray(match.keywords) ? match.keywords.map((item: any) => item.value).filter(Boolean) : [],
    toolLogic: match.toolLogic || 'ANY',
    toolNames: match.tools || [],
    headers
  };
}

function fromBackendActionConfig(
  actionType: Api.Gateway.RoutingActionType,
  action: Record<string, any>
): Api.Gateway.RoutingActionConfig {
  return {
    actionType,
    targetModel: action.targetModel || null,
    targetModelGroup:
      Array.isArray(action.models) && action.models.length ? action.models.join(',') : action.modelGroup || null,
    simpleModel: action.simpleTaskTargetModel || action.simpleModel || null,
    complexModel: action.complexModel || null,
    denyReason: action.denyReason || null
  };
}

function splitModels(value?: string | null) {
  return value
    ? value
        .split(',')
        .map(item => item.trim())
        .filter(Boolean)
    : [];
}

function normalizeStringList(value?: unknown): string[] {
  return Array.isArray(value) ? value.map(item => String(item || '').trim()).filter(Boolean) : [];
}

function fromBackendFallbackConfig(fallback: Record<string, any>): Api.Gateway.RoutingFallbackConfig {
  return {
    fallbackMode: fallback.onTargetUnavailable || fallback.onSourceUnavailable || 'ORIGINAL_MODEL',
    fallbackModels: fallback.fallbackModels || (fallback.fallbackModel ? [fallback.fallbackModel] : []),
    defaultAction: fallback.onClassifierError || 'ORIGINAL_MODEL'
  };
}
