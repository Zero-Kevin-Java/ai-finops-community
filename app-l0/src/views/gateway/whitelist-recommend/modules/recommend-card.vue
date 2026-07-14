<script setup lang="ts">
import { computed } from 'vue';
import { NButton, NCard, NDivider, NSpace, NTag } from 'naive-ui';
import { $t } from '@/locales';

defineOptions({ name: 'WhitelistRecommendCard' });

const props = defineProps<{
  item: Api.Gateway.WhitelistRecommendation;
  loading?: boolean;
}>();

const emit = defineEmits<{
  accept: [id: CommonType.IdType];
  reject: [id: CommonType.IdType];
}>();

const confidencePct = computed(() => {
  const val = props.item.avgConfidence;
  if (val == null) return '--';
  return (val * 100).toFixed(0);
});

const isPending = computed(() => props.item.status === 'pending');

const statusTag = computed(() => {
  switch (props.item.status) {
    case 'accepted':
      return { type: 'success' as const, label: $t('page.gateway.whitelistRecommend.accepted') };
    case 'rejected':
      return { type: 'default' as const, label: $t('page.gateway.whitelistRecommend.rejected') };
    case 'expired':
      return { type: 'warning' as const, label: $t('page.gateway.whitelistRecommend.expired') };
    default:
      return null;
  }
});

function handleAccept() {
  emit('accept', props.item.id);
}

function handleReject() {
  emit('reject', props.item.id);
}
</script>

<template>
  <NCard :bordered="false" size="small" class="recommend-card">
    <div class="flex flex-col gap-12px">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-8px">
          <span class="text-14px font-medium">{{ item.keyword }}</span>
          <NTag v-if="statusTag" :type="statusTag.type" size="small" round>
            {{ statusTag.label }}
          </NTag>
        </div>
        <span class="text-12px text-gray-400">{{ item.requestPath }}</span>
      </div>

      <div class="flex items-center gap-24px text-13px text-gray-500">
        <span>{{ $t('page.gateway.whitelistRecommend.requestCount', { count: item.requestCount }) }}</span>
        <span>{{ $t('page.gateway.whitelistRecommend.avgConfidence', { pct: confidencePct }) }}</span>
      </div>

      <div class="text-13px text-gray-600 leading-20px">{{ item.reason }}</div>

      <div class="flex items-start gap-8px">
        <span class="text-12px text-gray-400 flex-shrink-0">{{ $t('page.gateway.whitelistRecommend.pattern') }}:</span>
        <code class="text-12px px-6px py-2px bg-gray-100 rounded break-all">{{ item.recommendedPattern }}</code>
      </div>

      <template v-if="isPending">
        <NDivider />
        <NSpace justify="end" :size="8">
          <NButton size="small" :loading="loading" @click="handleReject">
            {{ $t('page.gateway.whitelistRecommend.ignore') }}
          </NButton>
          <NButton size="small" type="primary" :loading="loading" @click="handleAccept">
            {{ $t('page.gateway.whitelistRecommend.addToWhitelist') }}
          </NButton>
        </NSpace>
      </template>
    </div>
  </NCard>
</template>

<style scoped>
.recommend-card {
  transition: opacity 0.3s ease;
}
.recommend-card :deep(.n-card__content) {
  padding: 16px;
}
</style>
