import { request } from '@/service/request';

/** 获取首页真实数据总览 */
export function fetchGetLlmHomeOverview(params: Api.Llm.HomeOverviewSearchParams) {
  return request<Api.Llm.HomeOverview>({
    url: '/llm/home-overview',
    method: 'get',
    params
  });
}
