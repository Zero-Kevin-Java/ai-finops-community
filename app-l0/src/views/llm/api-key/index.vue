<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider, NTag } from 'naive-ui';
import {
  fetchBatchDeleteLlmApiKey,
  fetchGetLlmApiKeyList,
  fetchUpdateLlmApiKeyStatus
} from '@/service/api/llm/api-key';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { useDownload } from '@/hooks/business/download';
import { useDict } from '@/hooks/business/dict';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { handleCopy } from '@/utils/copy';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import DictTag from '@/components/custom/dict-tag.vue';
import StatusSwitch from '@/components/custom/status-switch.vue';
import ApiKeyOperateDrawer from './modules/api-key-operate-drawer.vue';
import ApiKeySearch from './modules/api-key-search.vue';

defineOptions({
  name: 'LlmApiKeyList'
});

useDict('llm_key_status');

const appStore = useAppStore();
const { download } = useDownload();
const { hasAuth } = useAuth();

const generatedKeyVisible = ref(false);
const generatedPlainKey = ref('');

const searchParams = ref<Api.Llm.ApiKeySearchParams>({
  pageNum: 1,
  pageSize: 10,
  clientId: null,
  ownerUserId: null,
  keyName: null,
  keyPrefix: null,
  status: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetLlmApiKeyList(searchParams.value),
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
        key: 'keyName',
        title: $t('page.llm.apiKey.keyName'),
        align: 'center',
        minWidth: 150,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'keyPrefix',
        title: $t('page.llm.apiKey.keyPrefix'),
        align: 'center',
        minWidth: 180,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'ownerUserName',
        title: $t('page.llm.apiKey.ownerUserId'),
        align: 'center',
        minWidth: 140,
        render: row => row.ownerUserName || '-'
      },
      {
        key: 'keyScope',
        title: $t('page.llm.apiKey.keyScope'),
        align: 'center',
        minWidth: 220,
        render: row => {
          const scopes = parseModelScope(row.keyScope);
          if (scopes.length === 0) return '-';
          return (
            <div class="flex-center flex-wrap gap-4px">
              {scopes.slice(0, 3).map(scope => (
                <NTag size="small" type={scope === '*' ? 'success' : 'info'}>
                  {scope === '*' ? '全部模型' : scope}
                </NTag>
              ))}
              {scopes.length > 3 && <NTag size="small">+{scopes.length - 3}</NTag>}
            </div>
          );
        }
      },
      {
        key: 'status',
        title: $t('page.llm.apiKey.status'),
        align: 'center',
        width: 90,
        render(row) {
          if (!hasAuth('llm:apiKey:edit')) {
            return <DictTag size="small" value={row.status} dictCode="llm_key_status" />;
          }

          return (
            <StatusSwitch
              value={row.status as Api.Common.EnableStatus}
              {...{
                'onUpdate:value': (value: Api.Common.EnableStatus) => {
                  row.status = value;
                }
              }}
              info={row.keyName}
              onSubmitted={(value, callback) => handleStatusChange(row, value, callback)}
            />
          );
        }
      },
      {
        key: 'expireTime',
        title: $t('page.llm.apiKey.expireTime'),
        align: 'center',
        minWidth: 160,
        ellipsis: {
          tooltip: true
        },
        render: row => row.expireTime || <NTag size="small" type="success">{$t('page.llm.apiKey.neverExpire')}</NTag>
      },
      {
        key: 'lastUsedTime',
        title: $t('page.llm.apiKey.lastUsedTime'),
        align: 'center',
        minWidth: 160,
        ellipsis: {
          tooltip: true
        }
      },
      {
        key: 'appName',
        title: $t('page.llm.apiKey.clientId'),
        align: 'center',
        minWidth: 140,
        ellipsis: {
          tooltip: true
        },
        render: row => row.appName || '-'
      },
      {
        key: 'createTime',
        title: $t('page.llm.apiKey.createTime'),
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
            if (!hasAuth('llm:apiKey:edit') || !hasAuth('llm:apiKey:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('llm:apiKey:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => edit(row.keyId!)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('llm:apiKey:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.keyId!)}
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
  useTableOperate(data, 'keyId', getData);

async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteLlmApiKey(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

async function handleDelete(keyId: CommonType.IdType) {
  const { error } = await fetchBatchDeleteLlmApiKey([keyId]);
  if (error) return;
  onDeleted();
}

async function edit(keyId: CommonType.IdType) {
  handleEdit(keyId);
}

function parseModelScope(value?: string | null): string[] {
  if (!value) return [];
  const trimmed = value.trim();
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed);
      return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
    } catch {
      return [];
    }
  }
  return trimmed
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
}

async function handleExport() {
  download('/llm/api-key/export', searchParams.value, `LLM_API_Key_${new Date().getTime()}.xlsx`);
}

function handleGeneratedKey(plainKey: string) {
  generatedPlainKey.value = plainKey;
  generatedKeyVisible.value = true;
}

async function handleStatusChange(
  row: Api.Llm.ApiKey,
  value: Api.Common.EnableStatus,
  callback: (flag: boolean) => void
) {
  const { error } = await fetchUpdateLlmApiKeyStatus({
    keyId: row.keyId,
    status: value
  });

  callback(!error);

  if (!error) {
    window.$message?.success($t('page.llm.apiKey.statusChangeSuccess'));
    getData();
  }
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ApiKeySearch v-model:model="searchParams" @search="getDataByPage" />
    <NCard :title="$t('page.llm.apiKey.title')" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('llm:apiKey:add')"
          :show-delete="hasAuth('llm:apiKey:remove')"
          :show-export="hasAuth('llm:apiKey:export')"
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
        :row-key="row => row.keyId"
        :scroll-x="scrollX"
        class="sm:h-full"
      />
      <ApiKeyOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @generated="handleGeneratedKey"
        @submitted="getDataByPage"
      />
    </NCard>

    <NModal v-model:show="generatedKeyVisible" preset="dialog" :title="$t('page.llm.apiKey.generatedKeyTitle')">
      <NSpace vertical :size="12">
        <NAlert type="warning" :show-icon="false">{{ $t('page.llm.apiKey.generatedKeyTip') }}</NAlert>
        <NInput id="tokenDetailInput" :value="generatedPlainKey" readonly type="textarea" :autosize="{ minRows: 2 }" />
        <NSpace justify="end">
          <NButton type="primary" @click="handleCopy(generatedPlainKey)">{{ $t('page.llm.apiKey.copyKey') }}</NButton>
        </NSpace>
      </NSpace>
    </NModal>
  </div>
</template>

<style scoped></style>
