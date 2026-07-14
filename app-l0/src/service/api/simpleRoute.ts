import { request } from '@/service/request';
import { buildSimpleRouteModelQuery } from './simpleRoute.shared';

/** 简单任务路由配置 */
export interface SimpleRouteVO {
  id: CommonType.IdType;
  tenantId: string;
  originalModel: string;
  targetModel: string;
  status: '0' | '1';
}

/** 可作为简单任务目标的模型 */
export interface SimpleRouteTargetVO {
  modelCode: string;
  displayName: string;
}

/** 查询单个模型的路由映射（用于 operate-drawer 加载） */
export function fetchGetSimpleRoute(originalModel: string) {
  return request<SimpleRouteVO>({
    ...buildSimpleRouteModelQuery(originalModel),
    method: 'get'
  });
}

/** 获取当前租户有价格配置的可选目标模型列表 */
export function fetchGetSimpleRouteTargets() {
  return request<SimpleRouteTargetVO[]>({
    url: '/api/simple-route/available-targets',
    method: 'get'
  });
}

/** 创建/更新简单任务路由（upsert，由 originalModel + tenantId 唯一确定） */
export function fetchSaveSimpleRoute(data: { originalModel: string; targetModel: string }) {
  return request<boolean>({
    url: '/api/simple-route/upsert',
    method: 'post',
    data
  });
}

/** 删除简单任务路由 */
export function fetchDeleteSimpleRoute(ids: CommonType.IdType[]) {
  return request<boolean>({
    url: `/api/simple-route/${ids.join(',')}`,
    method: 'delete'
  });
}
