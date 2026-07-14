<script setup lang="ts">
import { ref, useAttrs, watch } from 'vue';
import type { SelectProps } from 'naive-ui';
import { useLoading } from '@sa/hooks';
import { fetchGetLlmAppClientList } from '@/service/api/llm/app-client';

defineOptions({
  name: 'LlmAppClientSelect'
});

interface Props {
  projectId?: CommonType.IdType | null;
  [key: string]: any;
}

const props = defineProps<Props>();

const value = defineModel<CommonType.IdType | null>('value', { required: false });

const attrs: SelectProps = useAttrs();

const { loading, startLoading, endLoading } = useLoading();

const options = ref<CommonType.Option<CommonType.IdType>[]>([]);

async function getOptions() {
  startLoading();
  const params: Api.Llm.AppClientSearchParams = {
    pageNum: 1,
    pageSize: 1000
  };
  if (props.projectId) {
    params.projectId = props.projectId as number;
  }

  const { error, data } = await fetchGetLlmAppClientList(params);

  if (!error) {
    options.value = data.rows.map(item => ({
      label: item.appName ? `${item.appName} (${item.appCode})` : '未命名应用',
      value: item.clientId
    }));
  }
  endLoading();
}

watch(
  () => props.projectId,
  () => {
    getOptions();
  }
);

getOptions();
</script>

<template>
  <NSelect
    v-model:value="value"
    :loading="loading"
    :options="options"
    filterable
    v-bind="attrs"
    placeholder="请选择应用"
  />
</template>

<style scoped></style>
