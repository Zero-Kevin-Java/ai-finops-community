<script setup lang="ts">
import { $t } from '@/locales';

defineOptions({ name: 'CacheEntryDetailModal' });

interface Props {
  loading?: boolean;
  data?: Api.Llm.CacheEntryDetailVo | null;
}

defineProps<Props>();

const visible = defineModel<boolean>('visible', { default: false });
</script>

<template>
  <NModal v-model:show="visible" preset="card" :title="$t('page.gateway.cacheEntry.detail')" class="w-800px max-w-90%">
    <NSpace v-if="loading" vertical :size="16" align="center" class="py-48px">
      <NSpin size="medium" />
      <NText depth="3">加载中...</NText>
    </NSpace>

    <NSpace v-else-if="data" vertical :size="16">
      <NDescriptions :column="2" size="small" bordered>
        <NDescriptionsItem :label="$t('page.gateway.cacheEntry.modelCode')">{{ data.modelCode }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.gateway.cacheEntry.hitCount')">{{ data.hitCount }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.gateway.cacheEntry.tokenCount')">{{ data.tokenCount }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.gateway.cacheEntry.expiresAt')">{{ data.expiresAt }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.gateway.cacheEntry.promptHash')" :span="2">
          <NText code>{{ data.promptHash }}</NText>
        </NDescriptionsItem>
      </NDescriptions>

      <NFormItem :label="$t('page.gateway.cacheEntry.promptText')">
        <NInput :value="data.promptText" type="textarea" readonly :autosize="{ minRows: 3, maxRows: 10 }" />
      </NFormItem>

      <NFormItem :label="$t('page.gateway.cacheEntry.responseText')">
        <NInput :value="data.responseText" type="textarea" readonly :autosize="{ minRows: 5, maxRows: 20 }" />
      </NFormItem>
    </NSpace>

    <NEmpty v-else :description="$t('common.noData')" />

    <template #footer>
      <NButton @click="visible = false">{{ $t('page.gateway.cacheEntry.closeDetail') }}</NButton>
    </template>
  </NModal>
</template>
