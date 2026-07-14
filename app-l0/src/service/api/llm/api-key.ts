import { request } from '@/service/request';

/** 获取 LLM API Key 列表 */
export function fetchGetLlmApiKeyList(params?: Api.Llm.ApiKeySearchParams) {
  return request<Api.Llm.ApiKeyList>({
    url: '/llm/api-key/list',
    method: 'get',
    params
  });
}

/** 新增 LLM API Key，返回仅展示一次的明文 Key */
export function fetchCreateLlmApiKey(data: Api.Llm.ApiKeyOperateParams) {
  return request<string>({
    url: '/llm/api-key',
    method: 'post',
    data: normalizeApiKeyPayload(data)
  });
}

/** 修改 LLM API Key */
export function fetchUpdateLlmApiKey(data: Api.Llm.ApiKeyOperateParams) {
  return request<boolean>({
    url: '/llm/api-key',
    method: 'put',
    data: normalizeApiKeyPayload(data)
  });
}

/** 批量删除 LLM API Key */
export function fetchBatchDeleteLlmApiKey(keyIds: CommonType.IdType[]) {
  return request<boolean>({
    url: `/llm/api-key/${keyIds.join(',')}`,
    method: 'delete'
  });
}

/** 修改 LLM API Key 状态 */
export function fetchUpdateLlmApiKeyStatus(data: Api.Llm.ApiKeyStatusOperateParams) {
  return request<boolean>({
    url: '/llm/api-key/changeStatus',
    method: 'put',
    data
  });
}

function normalizeApiKeyPayload(data: Api.Llm.ApiKeyOperateParams) {
  return {
    ...data,
    keyScope: data.keyScope?.join(',') || ''
  };
}
