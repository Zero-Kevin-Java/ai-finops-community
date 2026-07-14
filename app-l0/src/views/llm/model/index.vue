<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider, NTag } from 'naive-ui';
import {
  fetchBatchDeleteLlmModel,
  fetchGetLlmModelList,
  fetchUpdateLlmModelStatus
} from '@/service/api/llm/model';
import { fetchGetLlmProviderOptions } from '@/service/api/llm/provider';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { useDownload } from '@/hooks/business/download';
import { useDict } from '@/hooks/business/dict';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import DictTag from '@/components/custom/dict-tag.vue';
import StatusSwitch from '@/components/custom/status-switch.vue';
import ModelDetailModal from './modules/model-detail-modal.vue';
import ModelOperateDrawer from './modules/model-operate-drawer.vue';
import ModelSearch from './modules/model-search.vue';
import { renderProviderLogo } from '../provider/modules/provider-logo';

defineOptions({
  name: 'LlmModelList'
});

useDict('sys_normal_disable');
useDict('llm_model_type');

const appStore = useAppStore();
const { download } = useDownload();
const { hasAuth } = useAuth();
const providerOptions = ref<Api.Llm.ProviderOption[]>([]);
const detailVisible = ref(false);
const detailModelId = ref<CommonType.IdType | null>(null);

const searchParams = ref<Api.Llm.ModelSearchParams>({
  pageNum: 1,
  pageSize: 10,
  modelCode: null,
  displayName: null,
  provider: null,
  supplier: null,
  modelType: null,
  status: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetLlmModelList(searchParams.value),
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
        key: 'modelCode',
        title: $t('page.llm.model.modelCode'),
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'displayName',
        title: $t('page.llm.model.displayName'),
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'provider',
        title: $t('page.llm.model.provider'),
        align: 'center',
        minWidth: 150,
        render(row) {
          const provider = findProviderOption(row.provider);

          return (
            <div class="flex-y-center justify-center gap-8px">
              {renderProviderLogo(provider || { providerName: row.provider }, 22)}
              <span>{row.provider}</span>
            </div>
          );
        }
      },
      {
        key: 'supplier',
        title: $t('page.llm.model.supplier'),
        align: 'center',
        minWidth: 130,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'protocol',
        title: $t('page.llm.model.protocol'),
        align: 'center',
        width: 120,
        render(row) {
          const isAnthropic = row.protocol === 'anthropic';
          return (
            <NTag size="small" type={isAnthropic ? 'warning' : 'info'} round>
              {isAnthropic ? 'Anthropic' : 'OpenAI 兼容'}
            </NTag>
          );
        }
      },
      {
        key: 'modelType',
        title: $t('page.llm.model.modelType'),
        align: 'center',
        minWidth: 110,
        render(row) {
          return <DictTag size="small" value={row.modelType} dictCode="llm_model_type" />;
        }
      },
      {
        key: 'status',
        title: $t('page.llm.model.status'),
        align: 'center',
        width: 90,
        render(row) {
          if (!hasAuth('llm:model:edit')) {
            return <DictTag size="small" value={row.status} dictCode="sys_normal_disable" />;
          }

          return (
            <StatusSwitch
              v-model:value={row.status}
              info={row.displayName}
              onSubmitted={(value, callback) => handleStatusChange(row, value, callback)}
            />
          );
        }
      },
      {
        key: 'remark',
        title: $t('page.llm.model.remark'),
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'createTime',
        title: $t('page.llm.model.createTime'),
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
        width: 208,
        fixed: 'right',
        render: row => {
          const divider = () => {
            if (!hasAuth('llm:model:edit') || !hasAuth('llm:model:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const detailBtn = () => {
            return (
              <ButtonIcon
                text
                type="info"
                icon="material-symbols:visibility-outline-rounded"
                tooltipContent="详情"
                onClick={() => openDetail(row)}
              />
            );
          };

          const editBtn = () => {
            if (!hasAuth('llm:model:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => edit(row.modelId!)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('llm:model:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.modelId!)}
              />
            );
          };

          return (
            <div class="flex-center gap-8px">
              {detailBtn()}
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
  useTableOperate(data, 'modelId', async () => {
    getData();
  });

async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteLlmModel(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

async function handleDelete(modelId: CommonType.IdType) {
  const { error } = await fetchBatchDeleteLlmModel([modelId]);
  if (error) return;
  onDeleted();
}

async function edit(modelId: CommonType.IdType) {
  handleEdit(modelId);
}

async function handleExport() {
  download('/llm/model/export', searchParams.value, `LLM模型_${new Date().getTime()}.xlsx`);
}

function openDetail(row: Api.Llm.Model) {
  detailModelId.value = row.modelId;
  detailVisible.value = true;
}

function findProviderOption(providerName?: string | null) {
  if (!providerName) return null;
  return providerOptions.value.find(item => item.providerName === providerName || item.label === providerName) || null;
}

async function loadProviderOptions() {
  const { data: options } = await fetchGetLlmProviderOptions();
  providerOptions.value = options || [];
}

async function handleStatusChange(
  row: Api.Llm.Model,
  value: Api.Common.EnableStatus,
  callback: (flag: boolean) => void
) {
  const { error } = await fetchUpdateLlmModelStatus({
    modelId: row.modelId,
    status: value
  });

  callback(!error);

  if (!error) {
    window.$message?.success($t('page.llm.model.statusChangeSuccess'));
    getData();
  }
}

loadProviderOptions();
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ModelSearch v-model:model="searchParams" @search="getDataByPage" />
    <NCard :title="$t('page.llm.model.title')" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('llm:model:add')"
          :show-delete="hasAuth('llm:model:remove')"
          :show-export="hasAuth('llm:model:export')"
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
        :row-key="row => row.modelId"
        :pagination="mobilePagination"
        class="sm:h-full"
      />
      <ModelOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @submitted="getData"
      />
      <ModelDetailModal
        v-model:visible="detailVisible"
        :model-id="detailModelId"
      />
    </NCard>
  </div>
</template>

<style scoped></style>
