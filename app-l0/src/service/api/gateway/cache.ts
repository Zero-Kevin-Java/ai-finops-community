import { request } from '@/service/request';

/**
 * 刷新网关缓存
 * @param type 缓存类型: apikey / model-access
 */
export function refreshCache(type: string) {
  return request<unknown>({
    url: '/api/gateway/cache/refresh',
    method: 'post',
    params: { type }
  });
}
