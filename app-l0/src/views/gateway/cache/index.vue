<script setup lang="ts">
import { ref } from 'vue';
import { useLoading } from '@sa/hooks';
import { $t } from '@/locales';
import { refreshCache as apiRefreshCache } from '@/service/api/gateway/cache';

defineOptions({
  name: 'CacheManagement'
});

interface CacheCard {
  type: string;
  title: string;
  icon: string;
  description: string;
  ttl: string;
  keyPattern: string;
  color: string;
}

const activeTab = ref('gateway');

const cacheCards = ref<CacheCard[]>([
  {
    type: 'apikey',
    title: 'API Key 缓存',
    icon: 'material-symbols:key-outline',
    description: 'API Key 验证结果缓存，减少控制面 HTTP 调用',
    ttl: '5 分钟',
    keyPattern: 'gateway:apikey:{sha256_hash}',
    color: 'success'
  },
  {
    type: 'model-access',
    title: '模型准入缓存',
    icon: 'material-symbols:shield-outline',
    description: '企业模型准入策略本地内存缓存，控制允许模型、禁止模型和默认准入模式',
    ttl: '10 分钟',
    keyPattern: 'gateway:model-access:{tenant_id}',
    color: 'warning'
  }
]);

const { loading, startLoading, endLoading } = useLoading();

async function refreshCache(type: string) {
  startLoading();
  try {
    await apiRefreshCache(type);
    window.$message?.success(`已发送 ${type} 缓存刷新通知`);
  } finally {
    endLoading();
  }
}
</script>

<template>
  <div class="h-full flex-col-stretch gap-12px overflow-hidden">
    <NTabs v-model:value="activeTab" type="line" class="flex-1-hidden">
      <NTabPane name="gateway" tab="网关缓存刷新">
        <NCard title="缓存管理" :bordered="false" size="small" class="card-wrapper">
          <template #header-extra>
            <NButton size="small" text @click="$t('common.refresh')">
              <template #icon>
                <icon-ic-round-refresh class="text-icon" />
              </template>
            </NButton>
          </template>

          <NAlert type="info" class="mb-16px">
            <template #header>缓存刷新机制</template>
            控制面数据变更时通过 Redis Pub/Sub（topic: gateway:cache:refresh）主动通知网关清除旧缓存。
            也可手动点击下方卡片的刷新按钮触发缓存刷新。
          </NAlert>

          <NGrid :cols="3" :x-gap="16" responsive="screen" item-responsive>
            <NGridItem v-for="card in cacheCards" :key="card.type">
              <NCard :bordered="false" size="small" hoverable class="cache-card">
                <template #header>
                  <NSpace align="center">
                    <SvgIcon :icon="card.icon" class="text-24px" />
                    <span>{{ card.title }}</span>
                  </NSpace>
                </template>
                <template #header-extra>
                  <NButton size="small" :loading="loading" @click="refreshCache(card.type)">
                    <template #icon>
                      <icon-ic-round-refresh class="text-icon" />
                    </template>
                    刷新
                  </NButton>
                </template>

                <NSpace vertical :size="12">
                  <NText depth="3">{{ card.description }}</NText>
                  <NDivider style="margin: 8px 0" />
                  <NDescriptions :column="1" size="small" bordered>
                    <NDescriptionsItem label="TTL">{{ card.ttl }}</NDescriptionsItem>
                    <NDescriptionsItem label="Redis Key">{{ card.keyPattern }}</NDescriptionsItem>
                  </NDescriptions>
                </NSpace>
              </NCard>
            </NGridItem>
          </NGrid>

          <NDivider />

          <NDescriptions :column="2" size="small" bordered>
            <NDescriptionsItem label="Pub/Sub Topic">
              <NText code>gateway:cache:refresh</NText>
            </NDescriptionsItem>
            <NDescriptionsItem label="消息格式">
              <NText code>{cacheType}:{cacheKey}</NText>
            </NDescriptionsItem>
            <NDescriptionsItem label="支持的 cacheType">
              <NSpace>
                <NTag type="success">apikey</NTag>
                <NTag type="warning">whitelist</NTag>
              </NSpace>
            </NDescriptionsItem>
            <NDescriptionsItem label="订阅器">GatewayCacheSubscriber（@PostConstruct 订阅）</NDescriptionsItem>
          </NDescriptions>
        </NCard>
      </NTabPane>
    </NTabs>
  </div>
</template>

<style scoped lang="scss">
.cache-card {
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }
}
</style>
