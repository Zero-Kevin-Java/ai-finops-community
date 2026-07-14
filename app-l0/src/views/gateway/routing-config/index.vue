<script setup lang="tsx">
import { onMounted, ref } from 'vue';
import { NDivider, NEllipsis, NSwitch, NTag } from 'naive-ui';
import {
  fetchBatchDeleteRoutingConfigRules,
  fetchCopyRoutingConfigRule,
  fetchGetRoutingConfigRuleInfo,
  fetchGetRoutingConfigRuleList,
  fetchGetRoutingConfigStats,
  fetchRefreshRoutingConfigCache,
  fetchUpdateRoutingConfigRuleStatus
} from '@/service/api/gateway';
import { fetchGetLlmApiKeyList, fetchGetLlmAppClientList } from '@/service/api/llm';
import { fetchGetDeptTree, fetchGetUserSelect } from '@/service/api/system';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import RoutingConfigOperateDrawer from './modules/routing-config-operate-drawer.vue';
import RoutingConfigSearch from './modules/routing-config-search.vue';
import RoutingConfigStats from './modules/routing-config-stats.vue';

defineOptions({
  name: 'GatewayRoutingConfig'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();
const deptNameMap = ref<Record<string, string>>({});
const userNameMap = ref<Record<string, string>>({});
const apiKeyNameMap = ref<Record<string, string>>({});
const appNameMap = ref<Record<string, string>>({});
const switchingRuleIds = ref<CommonType.IdType[]>([]);
const stats = ref<Api.Gateway.RoutingConfigStats | null>(null);
const statsLoading = ref(false);

const searchParams = ref<Api.Gateway.RoutingConfigSearchParams>({
  pageNum: 1,
  pageSize: 10,
  tenantId: null,
  ruleName: null,
  actionType: null,
  executionMode: null,
  status: null,
  params: {}
});

const actionText: Record<Api.Gateway.RoutingActionType, string> = {
  ORIGINAL_MODEL: '不拦截',
  TARGET_MODEL: '指定模型',
  MODEL_GROUP: '模型组',
  CLASSIFIER: '执行项目分类器',
  DENY: '拦截'
};

const executionModeText: Record<Api.Gateway.RoutingExecutionMode, string> = {
  ENFORCE: '生效执行',
  RECORD_ONLY: '仅记录不执行'
};

const fallbackModeText: Record<NonNullable<Api.Gateway.RoutingFallbackConfig['fallbackMode']>, string> = {
  ORIGINAL_MODEL: '回到原模型',
  TARGET_MODEL: '使用兜底模型',
  MODEL_GROUP: '模型组',
  DENY: '拦截'
};

const actionTagType: Record<Api.Gateway.RoutingActionType, NaiveUI.ThemeColor> = {
  ORIGINAL_MODEL: 'success',
  TARGET_MODEL: 'primary',
  MODEL_GROUP: 'info',
  CLASSIFIER: 'warning',
  DENY: 'error'
};

function formatEmpty(value?: string | number | null) {
  return value === null || value === undefined || value === '' ? '-' : value;
}

function renderText(value?: string | number | null) {
  return <NEllipsis tooltip>{formatEmpty(value)}</NEllipsis>;
}

function resolveName(value: string | number, map: Record<string, string>) {
  const key = String(value);
  return map[key] || key;
}

function formatLogic(logic?: Api.Gateway.RoutingConditionLogic) {
  return logic === 'ANY' ? '或' : '且';
}

function normalizeValues(values?: Array<string | number> | null) {
  return (values || []).map(item => String(item)).filter(Boolean);
}

function renderTagList(values: string[], map: Record<string, string>, type: NaiveUI.ThemeColor = 'default') {
  if (!values.length) return null;
  return (
    <div class="routing-value-tags">
      {values.map(value => (
        <NTag key={value} size="small" type={type} round>
          {resolveName(value, map)}
        </NTag>
      ))}
    </div>
  );
}

function renderConditionGroup(
  label: string,
  values: string[],
  map: Record<string, string>,
  type: NaiveUI.ThemeColor = 'default',
  suffix?: string
) {
  if (!values.length) return null;
  return (
    <div class="routing-condition-group">
      <span class="routing-condition-label">
        {label}
        {suffix ? `(${suffix})` : ''}
      </span>
      {renderTagList(values, map, type)}
    </div>
  );
}

function renderMatchSummary(row: Api.Gateway.RoutingConfigRule) {
  const match = row.matchConfig || {};
  const apiKeys = match.apiKeyIds?.length ? match.apiKeyIds : match.apiKeyId ? [match.apiKeyId] : [];
  const groups = [
    renderConditionGroup('部门', normalizeValues(match.departments), deptNameMap.value, 'info'),
    renderConditionGroup('个人', normalizeValues(match.userIds), userNameMap.value, 'success'),
    renderConditionGroup('应用', normalizeValues(match.appIds), appNameMap.value, 'primary'),
    renderConditionGroup('API Key', normalizeValues(apiKeys), apiKeyNameMap.value, 'warning'),
    renderConditionGroup('原模型', normalizeValues(match.originalModels), {}, 'default'),
    renderConditionGroup('关键词', normalizeValues(match.keywords), {}, 'error', formatLogic(match.keywordLogic)),
    renderConditionGroup('工具', normalizeValues(match.toolNames), {}, 'default', formatLogic(match.toolLogic))
  ].filter(Boolean);
  if (!groups.length) {
    return <span class="routing-empty-text">全局请求</span>;
  }
  return (
    <div class="routing-match-cell">
      <div class="routing-match-logic">
        <NTag size="small" type={match.logic === 'ANY' ? 'info' : 'default'} round>
          {match.logic === 'ANY' ? '任一条件' : '全部满足'}
        </NTag>
      </div>
      {groups}
    </div>
  );
}

function formatActionParam(row: Api.Gateway.RoutingConfigRule) {
  const action = row.actionConfig || { actionType: 'ORIGINAL_MODEL' as const };
  if (action.actionType === 'TARGET_MODEL') return action.targetModel || '-';
  if (action.actionType === 'MODEL_GROUP') return action.targetModelGroup || '-';
  if (action.actionType === 'DENY') return action.denyReason || '-';
  return '-';
}

function formatFallbackMode(row: Api.Gateway.RoutingConfigRule) {
  const fallback = row.fallbackConfig || {};
  return fallbackModeText[fallback.fallbackMode || 'ORIGINAL_MODEL'];
}

function formatFallbackModels(row: Api.Gateway.RoutingConfigRule) {
  const fallback = row.fallbackConfig || {};
  return fallback.fallbackModels?.length ? fallback.fallbackModels.join('、') : '-';
}

function isSwitching(ruleId: CommonType.IdType) {
  return switchingRuleIds.value.includes(ruleId);
}

function flattenDeptTree(nodes: Api.Common.CommonTreeRecord, result: Record<string, string> = {}) {
  nodes.forEach(node => {
    result[String(node.id)] = node.label;
    flattenDeptTree((node.children || []) as unknown as Api.Common.CommonTreeRecord, result);
  });
  return result;
}

async function loadRelationMaps() {
  const [{ data: depts }, { data: users }, { data: apiKeys }, { data: apps }] = await Promise.all([
    fetchGetDeptTree(),
    fetchGetUserSelect(),
    fetchGetLlmApiKeyList({ pageNum: 1, pageSize: 1000 }),
    fetchGetLlmAppClientList({ pageNum: 1, pageSize: 1000 })
  ]);

  deptNameMap.value = flattenDeptTree(depts || []);
  userNameMap.value = Object.fromEntries(
    (users || []).map(item => [
      String(item.userId),
      item.nickName ? `${item.nickName}（${item.userName}）` : item.userName
    ])
  );
  apiKeyNameMap.value = Object.fromEntries(
    (apiKeys?.rows || []).map(item => [
      String(item.keyId),
      item.keyName ? `${item.keyName}（${item.keyPrefix}）` : item.keyPrefix || String(item.keyId)
    ])
  );
  appNameMap.value = Object.fromEntries(
    (apps?.rows || []).map(item => [
      String(item.clientId),
      item.appName ? `${item.appName}（${item.appCode}）` : item.appCode || String(item.clientId)
    ])
  );
}

async function loadStats() {
  statsLoading.value = true;
  try {
    const { data, error } = await fetchGetRoutingConfigStats({ tenantId: searchParams.value.tenantId });
    if (error || !data) return;
    stats.value = data;
  } finally {
    statsLoading.value = false;
  }
}

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetRoutingConfigRuleList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      {
        type: 'selection',
        align: 'center',
        width: 46
      },
      {
        key: 'ruleName',
        title: '规则名称',
        align: 'center',
        minWidth: 160,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'priority',
        title: '优先级',
        align: 'center',
        width: 72
      },
      {
        key: 'remark',
        title: '备注信息',
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'status',
        title: '开启状态',
        align: 'center',
        width: 90,
        render: row => (
          <div class="routing-status-cell">
            <NSwitch
              value={row.status === '0'}
              loading={isSwitching(row.ruleId)}
              disabled={!hasAuth('gateway:whitelist:edit')}
              onUpdateValue={checked => handleChangeStatus(row, checked ? '0' : '1')}
            />
          </div>
        )
      },
      {
        key: 'matchConfig',
        title: '命中条件',
        align: 'left',
        minWidth: 540,
        render: row => renderMatchSummary(row)
      },
      {
        key: 'actionType',
        title: '命中后执行',
        align: 'center',
        width: 146,
        render: row => {
          const action = row.actionConfig?.actionType || row.actionType || 'ORIGINAL_MODEL';
          return (
            <NTag type={actionTagType[action]} round>
              {actionText[action]}
            </NTag>
          );
        }
      },
      {
        key: 'actionParam',
        title: '动作参数',
        align: 'center',
        minWidth: 150,
        render: row => renderText(formatActionParam(row))
      },
      {
        key: 'executionMode',
        title: '执行方式',
        align: 'center',
        width: 126,
        render: row => (
          <NTag type={row.executionMode === 'ENFORCE' ? 'success' : 'warning'} round>
            {executionModeText[row.executionMode]}
          </NTag>
        )
      },
      {
        key: 'fallbackMode',
        title: '兜底模型',
        align: 'center',
        width: 130,
        render: row => renderText(formatFallbackMode(row))
      },
      {
        key: 'fallbackModels',
        title: '兜底模型列表',
        align: 'center',
        minWidth: 150,
        render: row => renderText(formatFallbackModels(row))
      },
      {
        key: 'effectiveStart',
        title: '生效开始',
        align: 'center',
        width: 150,
        render: row => renderText(row.effectiveStart)
      },
      {
        key: 'effectiveEnd',
        title: '生效结束',
        align: 'center',
        width: 150,
        render: row => renderText(row.effectiveEnd)
      },
      {
        key: 'hitCount',
        title: '命中次数',
        align: 'center',
        width: 88,
        render: row => formatEmpty(row.hitCount)
      },
      {
        key: 'lastHitTime',
        title: '最近命中',
        align: 'center',
        width: 150,
        render: row => renderText(row.lastHitTime)
      },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 208,
        fixed: 'right',
        render: row => {
          const divider = () => {
            if (
              !hasAuth('gateway:whitelist:remove') ||
              (!hasAuth('gateway:whitelist:edit'))
            ) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('gateway:whitelist:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => edit(row.ruleId)}
              />
            );
          };

          const copyBtn = () => {
            if (!hasAuth('gateway:whitelist:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="success"
                icon="material-symbols:content-copy-outline"
                tooltipContent="复制"
                onClick={() => handleCopy(row.ruleId)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('gateway:whitelist:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.ruleId)}
              />
            );
          };

          return (
            <div class="flex-center gap-8px">
              {editBtn()}
              {copyBtn()}
              {divider()}
              {deleteBtn()}
            </div>
          );
        }
      }
    ]
  });

const { drawerVisible, operateType, editingData, handleAdd, handleEdit, checkedRowKeys, onBatchDeleted, onDeleted } =
  useTableOperate(data, 'ruleId', getData);

function refreshData() {
  getData();
  loadStats();
}

function refreshDataByPage() {
  getDataByPage();
  loadStats();
}

async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteRoutingConfigRules(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
  loadStats();
}

async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteRoutingConfigRules([id]);
  if (error) return;
  onDeleted();
  loadStats();
}

function edit(id: CommonType.IdType) {
  handleEdit(id);
}

async function handleCopy(id: CommonType.IdType) {
  const { error } = await fetchCopyRoutingConfigRule(id);
  if (error) return;
  window.$message?.success('路由配置已复制');
  refreshDataByPage();
}

async function handleChangeStatus(row: Api.Gateway.RoutingConfigRule, status: string) {
  if (isSwitching(row.ruleId)) return;
  switchingRuleIds.value = [...switchingRuleIds.value, row.ruleId];
  try {
    const { error } = await fetchUpdateRoutingConfigRuleStatus({
      ruleId: row.ruleId,
      tenantId: row.tenantId,
      status
    });
    if (error) return;
    row.status = status;
    window.$message?.success(status === '0' ? '规则已启用' : '规则已停用');
    refreshDataByPage();
  } finally {
    switchingRuleIds.value = switchingRuleIds.value.filter(id => id !== row.ruleId);
  }
}

async function handleRefreshCache() {
  const { error } = await fetchRefreshRoutingConfigCache();
  if (error) return;
  window.$message?.success('路由配置缓存刷新请求已发送');
}

onMounted(() => {
  loadRelationMaps();
  loadStats();
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <RoutingConfigStats :stats="stats" :loading="statsLoading" />
    <RoutingConfigSearch v-model:model="searchParams" @search="refreshDataByPage" />
    <NCard title="路由配置" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <NSpace align="center" :size="8">
          <NButton v-if="hasAuth('gateway:whitelist:edit')" size="small" secondary @click="handleRefreshCache">
            刷新路由缓存
          </NButton>
          <TableHeaderOperation
            v-model:columns="columnChecks"
            :disabled-delete="checkedRowKeys.length === 0"
            :loading="loading"
            :show-add="hasAuth('gateway:whitelist:add')"
            :show-delete="hasAuth('gateway:whitelist:remove')"
            @add="handleAdd"
            @delete="handleBatchDelete"
            @refresh="refreshData"
          />
        </NSpace>
      </template>
      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="data"
        size="small"
        :flex-height="!appStore.isMobile"
        :scroll-x="scrollX"
        :loading="loading"
        remote
        :row-key="row => row.ruleId"
        :pagination="mobilePagination"
        class="sm:h-full"
      />
      <RoutingConfigOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @submitted="refreshDataByPage"
      />
    </NCard>
  </div>
</template>

<style scoped lang="scss">
:deep(.n-data-table-wrapper),
:deep(.n-data-table-base-table),
:deep(.n-data-table-base-table-body) {
  height: 100%;
}

:deep(.n-data-table-th),
:deep(.n-data-table-td) {
  vertical-align: middle;
}

.routing-match-cell {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-height: 32px;
  padding: 2px 0;
}

.routing-condition-group {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  align-items: center;
  gap: 6px;
}

.routing-condition-label {
  color: #667085;
  font-size: 12px;
  line-height: 22px;
  white-space: nowrap;
}

.routing-value-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
}

.routing-empty-text {
  color: #98a2b3;
}

.routing-status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
}
</style>
