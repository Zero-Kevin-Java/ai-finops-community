import { request } from '@/service/request';

export interface ApiKeyValidateResult {
  tenantId: string;
  apiKeyId: string;
  keyMasked: string;
  status: string;
  teamTag: string | null;
}

/**
 * 验证 API Key（网关内部调用）
 * Day 2 阶段使用 Mock，Day 5 后对接真实 API
 */
export function fetchValidateApiKey(key: string) {
  return request<ApiKeyValidateResult>({
    url: '/api/gateway/api-keys/validate',
    method: 'get',
    params: { key }
  });
}

/**
 * 获取 API Key 列表
 */
export function fetchGetApiKeyList(params?: Api.Gateway.ApiKeySearchParams) {
  return request<Api.Gateway.ApiKeyList>({
    url: '/api/gateway/api-keys/list',
    method: 'get',
    params
  });
}

/**
 * 新增 API Key
 */
export function fetchCreateApiKey(data: Api.Gateway.ApiKeyOperateParams) {
  return request<boolean>({
    url: '/api/gateway/api-keys',
    method: 'post',
    data
  });
}

/**
 * 修改 API Key
 */
export function fetchUpdateApiKey(data: Api.Gateway.ApiKeyOperateParams) {
  return request<boolean>({
    url: '/api/gateway/api-keys',
    method: 'put',
    data
  });
}

/**
 * 批量删除 API Key
 */
export function fetchBatchDeleteApiKeys(ids: number[]) {
  return request<boolean>({
    url: `/api/gateway/api-keys/${ids.join(',')}`,
    method: 'delete'
  });
}
