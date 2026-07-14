import type { RouteComponent } from 'vue-router';

/**
 * Backend menu data may come from older RuoYi SQL scripts where component paths
 * no longer match the generated Elegant Router view keys.
 */
export const legacyViews: Record<string, RouteComponent | (() => Promise<RouteComponent>)> = {
  'system_user_auth-role': () => import('@/views/system/user/index.vue'),
  'system_role_auth-user': () => import('@/views/system/role/index.vue'),
  system_dict_data: () => import('@/views/system/dict/index.vue'),
  system_oss_config: () => import('@/views/system/oss-config/index.vue'),
  monitor_admin: () => import('./legacy-route-placeholder.vue'),
  monitor_snailjob: () => import('./legacy-route-placeholder.vue'),
  llm_app: () => import('@/views/llm/app-client/index.vue'),
  gateway_whitelist_recommend: () => import('@/views/gateway/whitelist-recommend/index.vue')
};
