import { request } from '@/service/request';

export interface WhitelistRule {
  policyId: number;
  tenantId: string;
  policyName: string;
  defaultMode: string;
  allowedModels: string;
  deniedModels: string;
  allowedModelCount: number;
  deniedModelCount: number;
  status: string;
  effectiveStart: string | null;
  effectiveEnd: string | null;
  remark: string;
}

/**
 * 获取租户当前有效模型准入配置
 */
export function fetchGetActiveWhitelistRules(tenantId: string) {
  return request<WhitelistRule>({
    url: `/api/gateway/model-access/${tenantId}/active`,
    method: 'get'
  });
}

/**
 * 获取模型准入配置列表
 */
export function fetchGetWhitelistRuleList(params?: Api.Gateway.WhitelistSearchParams) {
  return request<Api.Gateway.WhitelistList>({
    url: '/api/gateway/model-access/list',
    method: 'get',
    params
  });
}

/**
 * 获取模型准入配置详情
 */
export function fetchGetWhitelistRuleInfo(policyId: number) {
  return request<Api.Gateway.WhitelistItem>({
    url: `/api/gateway/model-access/${policyId}`,
    method: 'get'
  });
}

/**
 * 新增模型准入配置
 */
export function fetchCreateWhitelistRule(data: Api.Gateway.WhitelistOperateParams) {
  return request<boolean>({
    url: '/api/gateway/model-access',
    method: 'post',
    data: normalizeModelAccessPayload(data)
  });
}

/**
 * 修改模型准入配置
 */
export function fetchUpdateWhitelistRule(data: Api.Gateway.WhitelistOperateParams) {
  return request<boolean>({
    url: '/api/gateway/model-access',
    method: 'put',
    data: normalizeModelAccessPayload(data)
  });
}

/**
 * 修改模型准入配置状态
 */
export function fetchUpdateWhitelistRuleStatus(
  data: Pick<Api.Gateway.WhitelistOperateParams, 'policyId' | 'tenantId' | 'status'>
) {
  return request<boolean>({
    url: '/api/gateway/model-access/status',
    method: 'put',
    data
  });
}

/**
 * 批量删除模型准入配置
 */
export function fetchBatchDeleteWhitelistRules(ids: CommonType.IdType[]) {
  return request<boolean>({
    url: `/api/gateway/model-access/${ids.join(',')}`,
    method: 'delete'
  });
}

/**
 * 刷新模型准入网关缓存
 */
export function fetchRefreshModelAccessCache() {
  return request<boolean>({
    url: '/api/gateway/model-access/cache/refresh',
    method: 'post'
  });
}

function normalizeModelAccessPayload(data: Api.Gateway.WhitelistOperateParams) {
  return {
    ...data,
    allowedModels: JSON.stringify(data.allowedModels || []),
    deniedModels: JSON.stringify(data.deniedModels || [])
  };
}
