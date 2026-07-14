import { request } from '@/service/request';

/** 获取 LLM 模型列表 */
export function fetchGetLlmModelList(params?: Api.Llm.ModelSearchParams) {
  return request<Api.Llm.ModelList>({
    url: '/llm/model/list',
    method: 'get',
    params
  });
}

/** 获取 LLM 模型详情 */
export function fetchGetLlmModelDetail(modelId: CommonType.IdType) {
  return request<Api.Llm.ModelDetail>({
    url: `/llm/model/detail/${modelId}`,
    method: 'get'
  });
}

/** 新增 LLM 模型 */
export function fetchCreateLlmModel(data: Api.Llm.ModelOperateParams) {
  return request<boolean>({
    url: '/llm/model',
    method: 'post',
    data
  });
}

/** 修改 LLM 模型 */
export function fetchUpdateLlmModel(data: Api.Llm.ModelOperateParams) {
  return request<boolean>({
    url: '/llm/model',
    method: 'put',
    data
  });
}

/** 批量删除 LLM 模型 */
export function fetchBatchDeleteLlmModel(modelIds: CommonType.IdType[]) {
  return request<boolean>({
    url: `/llm/model/${modelIds.join(',')}`,
    method: 'delete'
  });
}

/** 修改 LLM 模型状态 */
export function fetchUpdateLlmModelStatus(data: Api.Llm.ModelStatusOperateParams) {
  return request<boolean>({
    url: '/llm/model/changeStatus',
    method: 'put',
    data
  });
}

/** 获取模型下拉选项（用于价格管理等表单） */
export function fetchGetLlmModelOptions() {
  return request<Api.Llm.ModelOption[]>({
    url: '/llm/model/options',
    method: 'get'
  });
}
