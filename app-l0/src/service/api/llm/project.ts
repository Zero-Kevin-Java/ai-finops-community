import { request } from '@/service/request';

/** 获取 LLM 项目列表 */
export function fetchGetLlmProjectList(params?: Api.Llm.ProjectSearchParams) {
  return request<Api.Llm.ProjectList>({
    url: '/llm/project/list',
    method: 'get',
    params
  });
}

/** 新增 LLM 项目 */
export function fetchCreateLlmProject(data: Api.Llm.ProjectOperateParams) {
  return request<boolean>({
    url: '/llm/project',
    method: 'post',
    data
  });
}

/** 修改 LLM 项目 */
export function fetchUpdateLlmProject(data: Api.Llm.ProjectOperateParams) {
  return request<boolean>({
    url: '/llm/project',
    method: 'put',
    data
  });
}

/** 批量删除 LLM 项目 */
export function fetchBatchDeleteLlmProject(projectIds: CommonType.IdType[]) {
  return request<boolean>({
    url: `/llm/project/${projectIds.join(',')}`,
    method: 'delete'
  });
}

/** 修改 LLM 项目状态 */
export function fetchUpdateLlmProjectStatus(data: Api.Llm.ProjectStatusOperateParams) {
  return request<boolean>({
    url: '/llm/project/changeStatus',
    method: 'put',
    data
  });
}
