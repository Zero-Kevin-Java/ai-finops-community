import { request } from '@/service/request';

/**
 * 获取白名单智能推荐列表
 */
export function fetchGetWhitelistRecommendList() {
  return request<Api.Gateway.WhitelistRecommendation[]>({
    url: '/api/whitelist/recommend',
    method: 'get'
  });
}

/**
 * 接受一条白名单推荐
 */
export function fetchAcceptWhitelistRecommend(id: CommonType.IdType) {
  return request<Api.Gateway.WhitelistRecommendAcceptResult>({
    url: `/api/whitelist/recommend/${id}/accept`,
    method: 'post'
  });
}

/**
 * 拒绝一条白名单推荐
 */
export function fetchRejectWhitelistRecommend(id: CommonType.IdType) {
  return request<null>({
    url: `/api/whitelist/recommend/${id}/reject`,
    method: 'post'
  });
}
