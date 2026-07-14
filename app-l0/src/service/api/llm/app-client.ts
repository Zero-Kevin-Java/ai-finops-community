import { request } from '@/service/request';

/** 获取 LLM 应用客户端列表 */
export function fetchGetLlmAppClientList(params?: Api.Llm.AppClientSearchParams) {
  return request<Api.Llm.AppClientList>({
    url: '/llm/app-client/list',
    method: 'get',
    params
  });
}

/** 新增 LLM 应用客户端 */
export function fetchCreateLlmAppClient(data: Api.Llm.AppClientOperateParams) {
  return request<boolean>({
    url: '/llm/app-client',
    method: 'post',
    data
  });
}

/** 修改 LLM 应用客户端 */
export function fetchUpdateLlmAppClient(data: Api.Llm.AppClientOperateParams) {
  return request<boolean>({
    url: '/llm/app-client',
    method: 'put',
    data
  });
}

/** 批量删除 LLM 应用客户端 */
export function fetchBatchDeleteLlmAppClient(clientIds: CommonType.IdType[]) {
  return request<boolean>({
    url: `/llm/app-client/${clientIds.join(',')}`,
    method: 'delete'
  });
}

/** 修改 LLM 应用客户端状态 */
export function fetchUpdateLlmAppClientStatus(data: Api.Llm.AppClientStatusOperateParams) {
  return request<boolean>({
    url: '/llm/app-client/changeStatus',
    method: 'put',
    data
  });
}
