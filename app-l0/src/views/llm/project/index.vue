<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider, NEllipsis } from 'naive-ui';
import {
  fetchBatchDeleteLlmProject,
  fetchGetLlmProjectList,
  fetchUpdateLlmProjectStatus
} from '@/service/api/llm/project';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { useDownload } from '@/hooks/business/download';
import { useDict } from '@/hooks/business/dict';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import DictTag from '@/components/custom/dict-tag.vue';
import StatusSwitch from '@/components/custom/status-switch.vue';
import ProjectOperateDrawer from './modules/project-operate-drawer.vue';
import ProjectSearch from './modules/project-search.vue';

defineOptions({
  name: 'LlmProjectList'
});

useDict('sys_normal_disable');

const appStore = useAppStore();
const { download } = useDownload();
const { hasAuth } = useAuth();

const searchParams = ref<Api.Llm.ProjectSearchParams>({
  pageNum: 1,
  pageSize: 10,
  projectCode: null,
  projectName: null,
  ownerUserId: null,
  status: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetLlmProjectList(searchParams.value),
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
        key: 'projectCode',
        title: $t('page.llm.project.projectCode'),
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'projectName',
        title: $t('page.llm.project.projectName'),
        align: 'center',
        minWidth: 160,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'ownerUserName',
        title: $t('page.llm.project.ownerUserId'),
        align: 'center',
        minWidth: 140,
        render: row => <NEllipsis>{row.ownerUserName || '-'}</NEllipsis>
      },
      {
        key: 'status',
        title: $t('page.llm.project.status'),
        align: 'center',
        width: 90,
        render(row) {
          if (!hasAuth('llm:project:edit')) {
            return <DictTag size="small" value={row.status} dictCode="sys_normal_disable" />;
          }

          return (
            <StatusSwitch
              v-model:value={row.status}
              info={row.projectName}
              onSubmitted={(value, callback) => handleStatusChange(row, value, callback)}
            />
          );
        }
      },
      {
        key: 'remark',
        title: $t('page.llm.project.remark'),
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'createTime',
        title: $t('page.llm.project.createTime'),
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
            if (!hasAuth('llm:project:edit') || !hasAuth('llm:project:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('llm:project:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => edit(row.projectId!)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('llm:project:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.projectId!)}
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
  useTableOperate(data, 'projectId', getData);

async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteLlmProject(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

async function handleDelete(projectId: CommonType.IdType) {
  const { error } = await fetchBatchDeleteLlmProject([projectId]);
  if (error) return;
  onDeleted();
}

async function edit(projectId: CommonType.IdType) {
  handleEdit(projectId);
}

async function handleExport() {
  download('/llm/project/export', searchParams.value, `项目列表_${new Date().getTime()}.xlsx`);
}

async function handleStatusChange(
  row: Api.Llm.Project,
  value: Api.Common.EnableStatus,
  callback: (flag: boolean) => void
) {
  const { error } = await fetchUpdateLlmProjectStatus({
    projectId: row.projectId,
    status: value
  });

  callback(!error);

  if (!error) {
    window.$message?.success($t('page.llm.project.statusChangeSuccess'));
    getData();
  }
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ProjectSearch v-model:model="searchParams" @search="getDataByPage" />
    <NCard :title="$t('page.llm.project.title')" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('llm:project:add')"
          :show-delete="hasAuth('llm:project:remove')"
          :show-export="hasAuth('llm:project:export')"
          @add="handleAdd"
          @delete="handleBatchDelete"
          @export="handleExport"
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
        :row-key="row => row.projectId"
        :pagination="mobilePagination"
        class="sm:h-full"
      />
      <ProjectOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @submitted="getData"
      />
    </NCard>
  </div>
</template>

<style scoped></style>
