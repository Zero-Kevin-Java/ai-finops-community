<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider } from 'naive-ui';
import {
  fetchBatchDeleteLlmAppClient,
  fetchGetLlmAppClientList,
  fetchUpdateLlmAppClientStatus
} from '@/service/api/llm/app-client';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { useDownload } from '@/hooks/business/download';
import { useDict } from '@/hooks/business/dict';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import DictTag from '@/components/custom/dict-tag.vue';
import StatusSwitch from '@/components/custom/status-switch.vue';
import AppClientOperateDrawer from './modules/app-client-operate-drawer.vue';
import AppClientSearch from './modules/app-client-search.vue';

defineOptions({
  name: 'LlmAppClientList'
});

useDict('llm_app_type');
useDict('sys_normal_disable');

const appStore = useAppStore();
const { download } = useDownload();
const { hasAuth } = useAuth();

const searchParams = ref<Api.Llm.AppClientSearchParams>({
  pageNum: 1,
  pageSize: 10,
  projectId: null,
  appCode: null,
  appName: null,
  appType: null,
  status: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetLlmAppClientList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      {
        type: 'selection',
        align: 'center',
        width: 48
      },
      {
        key: 'index',
        title: $t('common.index'),
        align: 'center',
        width: 64,
        render: (_, index) => index + 1
      },
      {
        key: 'projectName',
        title: $t('page.llm.appClient.projectId'),
        align: 'center',
        minWidth: 150,
        ellipsis: {
          tooltip: true
        },
        render: row => row.projectName || '-'
      },
      {
        key: 'appCode',
        title: $t('page.llm.appClient.appCode'),
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'appName',
        title: $t('page.llm.appClient.appName'),
        align: 'center',
        minWidth: 160,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'appType',
        title: $t('page.llm.appClient.appType'),
        align: 'center',
        width: 120,
        render: row => <DictTag size="small" value={row.appType} dictCode="llm_app_type" />
      },
      {
        key: 'status',
        title: $t('page.llm.appClient.status'),
        align: 'center',
        width: 90,
        render(row) {
          if (!hasAuth('llm:appClient:edit')) {
            return <DictTag size="small" value={row.status} dictCode="sys_normal_disable" />;
          }

          return (
            <StatusSwitch
              v-model:value={row.status}
              info={row.appName}
              onSubmitted={(value, callback) => handleStatusChange(row, value, callback)}
            />
          );
        }
      },
      {
        key: 'createTime',
        title: $t('page.llm.appClient.createTime'),
        align: 'center',
        minWidth: 160,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 130,
        render: row => {
          const divider = () => {
            if (!hasAuth('llm:appClient:edit') || !hasAuth('llm:appClient:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('llm:appClient:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => edit(row.clientId!)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('llm:appClient:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.clientId!)}
              />
            );
          };

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
  useTableOperate(data, 'clientId', getData);

async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteLlmAppClient(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

async function handleDelete(clientId: CommonType.IdType) {
  const { error } = await fetchBatchDeleteLlmAppClient([clientId]);
  if (error) return;
  onDeleted();
}

async function edit(clientId: CommonType.IdType) {
  handleEdit(clientId);
}

async function handleExport() {
  download('/llm/app-client/export', searchParams.value, `LLM应用客户端_${new Date().getTime()}.xlsx`);
}

async function handleStatusChange(
  row: Api.Llm.AppClient,
  value: Api.Common.EnableStatus,
  callback: (flag: boolean) => void
) {
  const { error } = await fetchUpdateLlmAppClientStatus({
    clientId: row.clientId,
    status: value
  });

  callback(!error);

  if (!error) {
    window.$message?.success($t('page.llm.appClient.statusChangeSuccess'));
    getData();
  }
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <AppClientSearch v-model:model="searchParams" @search="getDataByPage" />
    <NCard :title="$t('page.llm.appClient.title')" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('llm:appClient:add')"
          :show-delete="hasAuth('llm:appClient:remove')"
          :show-export="hasAuth('llm:appClient:export')"
          @add="handleAdd"
          @delete="handleBatchDelete"
          @export="handleExport"
          @refresh="getData"
        />
      </template>
      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
        remote
        striped
        size="small"
        :columns="columns"
        :data="data"
        :flex-height="!appStore.isMobile"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.clientId"
        :scroll-x="scrollX"
        class="sm:h-full"
      />
      <AppClientOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @submitted="getDataByPage"
      />
    </NCard>
  </div>
</template>

<style scoped></style>
