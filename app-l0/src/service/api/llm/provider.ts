import { request } from '@/service/request';

/** 获取 LLM 供应商列表 */
export function fetchGetLlmProviderList(params?: Api.Llm.ProviderSearchParams) {
  return request<Api.Llm.ProviderList>({
    url: '/llm/provider/list',
    method: 'get',
    params
  });
}

/** 新增 LLM 供应商 */
export function fetchCreateLlmProvider(data: Api.Llm.ProviderOperateParams) {
  return request<boolean>({
    url: '/llm/provider',
    method: 'post',
    data
  });
}

/** 修改 LLM 供应商 */
export function fetchUpdateLlmProvider(data: Api.Llm.ProviderOperateParams) {
  return request<boolean>({
    url: '/llm/provider',
    method: 'put',
    data
  });
}

/** 批量删除 LLM 供应商 */
export function fetchBatchDeleteLlmProvider(providerIds: CommonType.IdType[]) {
  return request<boolean>({
    url: `/llm/provider/${providerIds.join(',')}`,
    method: 'delete'
  });
}

/** 修改 LLM 供应商状态 */
export function fetchUpdateLlmProviderStatus(data: Api.Llm.ProviderStatusOperateParams) {
  return request<boolean>({
    url: '/llm/provider/changeStatus',
    method: 'put',
    data
  });
}

/** 获取 LLM 供应商下拉选项 */
export function fetchGetLlmProviderOptions() {
  return request<Api.Llm.ProviderOption[]>({
    url: '/llm/provider/options',
    method: 'get'
  });
}

/** 按模型名称匹配 LLM 供应商 */
export function fetchMatchLlmProviderByModelName(modelName: string) {
  return request<Api.Llm.Provider | null>({
    url: '/llm/provider/match',
    method: 'get',
    params: { modelName }
  });
}
