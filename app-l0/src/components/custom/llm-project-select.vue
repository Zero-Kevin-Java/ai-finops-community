<script setup lang="ts">
import { ref, useAttrs } from 'vue';
import type { SelectProps } from 'naive-ui';
import { useLoading } from '@sa/hooks';
import { fetchGetLlmProjectList } from '@/service/api/llm/project';

defineOptions({
  name: 'LlmProjectSelect'
});

interface Props {
  [key: string]: any;
}

defineProps<Props>();

const value = defineModel<CommonType.IdType | null>('value', { required: false });

const attrs: SelectProps = useAttrs();

const { loading, startLoading, endLoading } = useLoading();

const options = ref<CommonType.Option<CommonType.IdType>[]>([]);

async function getOptions() {
  startLoading();
  const { error, data } = await fetchGetLlmProjectList({
    pageNum: 1,
    pageSize: 1000
  });

  if (!error) {
    options.value = data.rows.map(item => ({
      label: item.projectName ? `${item.projectName} (${item.projectCode})` : '未命名项目',
      value: item.projectId
    }));
  }
  endLoading();
}

getOptions();
</script>

<template>
  <NSelect
    v-model:value="value"
    :loading="loading"
    :options="options"
    filterable
    v-bind="attrs"
    placeholder="请选择项目"
  />
</template>

<style scoped></style>
