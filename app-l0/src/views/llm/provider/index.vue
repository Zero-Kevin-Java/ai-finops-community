<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider, NTag } from 'naive-ui';
import {
  fetchBatchDeleteLlmProvider,
  fetchGetLlmProviderList,
  fetchUpdateLlmProviderStatus
} from '@/service/api/llm/provider';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { useDict } from '@/hooks/business/dict';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import DictTag from '@/components/custom/dict-tag.vue';
import StatusSwitch from '@/components/custom/status-switch.vue';
import ProviderOperateDrawer from './modules/provider-operate-drawer.vue';
import ProviderSearch from './modules/provider-search.vue';
import { renderProviderLogo } from './modules/provider-logo';

defineOptions({
  name: 'LlmProviderList'
});

useDict('sys_normal_disable');

const appStore = useAppStore();
const { hasAuth } = useAuth();

const searchParams = ref<Api.Llm.ProviderSearchParams>({
  pageNum: 1,
  pageSize: 10,
  providerName: null,
  status: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetLlmProviderList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      { key: 'index', title: $t('common.index'), align: 'center', width: 64, render: (_, index) => index + 1 },
      {
        key: 'logo',
        title: $t('page.llm.provider.logo'),
        align: 'center',
        width: 80,
        render: row => renderProviderLogo(row)
      },
      {
        key: 'providerName',
        title: $t('page.llm.provider.providerName'),
        align: 'center',
        minWidth: 140,
        ellipsis: { tooltip: true }
      },
      {
        key: 'modelPrefixes',
        title: $t('page.llm.provider.modelPrefixes'),
        align: 'left',
        minWidth: 220,
        render: row => {
          const prefixes = parseModelPrefixes(row.modelPrefixes);
          if (!prefixes.length) return null;

          return (
            <div class="flex flex-wrap gap-6px">
              {prefixes.map(prefix => (
                <NTag size="small" bordered={false} type="info">
                  {prefix}
                </NTag>
              ))}
            </div>
          );
        }
      },
      { key: 'sortOrder', title: $t('page.llm.provider.sortOrder'), align: 'center', width: 90 },
      {
        key: 'status',
        title: $t('page.llm.provider.status'),
        align: 'center',
        width: 90,
        render(row) {
          if (!hasAuth('llm:provider:edit')) {
            return <DictTag size="small" value={row.status} dictCode="sys_normal_disable" />;
          }

          return (
            <StatusSwitch
              v-model:value={row.status}
              info={row.providerName}
              onSubmitted={(value, callback) => handleStatusChange(row, value, callback)}
            />
          );
        }
      },
      {
        key: 'createTime',
        title: $t('page.llm.provider.createTime'),
        align: 'center',
        minWidth: 160,
        ellipsis: { tooltip: true }
      },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 130,
        render: row => {
          const divider = () => (!hasAuth('llm:provider:edit') || !hasAuth('llm:provider:remove') ? null : <NDivider vertical />);
          const editBtn = () =>
            !hasAuth('llm:provider:edit') ? null : (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => edit(row.providerId!)}
              />
            );
          const deleteBtn = () =>
            !hasAuth('llm:provider:remove') ? null : (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.providerId!)}
              />
            );
          return (
            <div class="flex-center gap-8px">
              {editBtn()}
              {divider()}
              {deleteBtn()}
            </div>
          );
        }
      }
    ]
  });

const { drawerVisible, operateType, editingData, handleAdd, handleEdit, checkedRowKeys, onBatchDeleted, onDeleted } =
  useTableOperate(data, 'providerId', getData);

async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteLlmProvider(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

async function handleDelete(providerId: CommonType.IdType) {
  const { error } = await fetchBatchDeleteLlmProvider([providerId]);
  if (error) return;
  onDeleted();
}

function edit(providerId: CommonType.IdType) {
  handleEdit(providerId);
}

function parseModelPrefixes(value?: string | null) {
  return (value || '')
    .split(/[,，;；、\n\r\t]+/)
    .map(item => item.trim())
    .filter(Boolean);
}

async function handleStatusChange(row: Api.Llm.Provider, value: Api.Common.EnableStatus, callback: (flag: boolean) => void) {
  const { error } = await fetchUpdateLlmProviderStatus({ providerId: row.providerId, status: value });
  callback(!error);

  if (!error) {
    window.$message?.success($t('page.llm.provider.statusChangeSuccess'));
    getData();
  }
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ProviderSearch v-model:model="searchParams" @search="getDataByPage" />
    <NCard :title="$t('page.llm.provider.title')" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('llm:provider:add')"
          :show-delete="hasAuth('llm:provider:remove')"
          @add="handleAdd"
          @delete="handleBatchDelete"
          @refresh="getData"
        />
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
        :row-key="row => row.providerId"
        :pagination="mobilePagination"
        class="sm:h-full"
      />
      <ProviderOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @submitted="getData"
      />
    </NCard>
  </div>
</template>
