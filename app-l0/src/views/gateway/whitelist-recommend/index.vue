<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NEmpty, NSpin } from 'naive-ui';
import {
  fetchAcceptWhitelistRecommend,
  fetchGetWhitelistRecommendList,
  fetchRejectWhitelistRecommend
} from '@/service/api/gateway';
import { $t } from '@/locales';
import RecommendCard from './modules/recommend-card.vue';

defineOptions({ name: 'GatewayWhitelistRecommend' });

const loading = ref(false);
const actionLoading = ref(false);
const list = ref<Api.Gateway.WhitelistRecommendation[]>([]);

async function loadList() {
  loading.value = true;
  try {
    const { data, error } = await fetchGetWhitelistRecommendList();
    if (!error && data) {
      list.value = data;
    }
  } finally {
    loading.value = false;
  }
}

async function handleAccept(id: CommonType.IdType) {
  actionLoading.value = true;
  try {
    const { data, error } = await fetchAcceptWhitelistRecommend(id);
    if (!error && data) {
      window.$message?.success($t('page.gateway.whitelistRecommend.acceptSuccess'));
      const item = list.value.find(r => r.id === id);
      if (item) {
        item.status = 'accepted';
        item.acceptedRuleId = data.ruleId;
      }
    } else if (!error) {
      window.$message?.error($t('page.gateway.whitelistRecommend.acceptFail'));
    }
  } finally {
    actionLoading.value = false;
  }
}

async function handleReject(id: CommonType.IdType) {
  actionLoading.value = true;
  try {
    const { error } = await fetchRejectWhitelistRecommend(id);
    if (!error) {
      window.$message?.success($t('page.gateway.whitelistRecommend.ignoreSuccess'));
      const item = list.value.find(r => r.id === id);
      if (item) {
        item.status = 'rejected';
      }
    }
  } finally {
    actionLoading.value = false;
  }
}

onMounted(() => {
  loadList();
});
</script>

<template>
  <div class="h-full flex-col-stretch gap-12px overflow-hidden lt-sm:overflow-auto">
    <NCard :title="$t('page.gateway.whitelistRecommend.cardTitle')" :bordered="false" size="small" class="card-wrapper">
      <NSpin :show="loading">
        <div v-if="list.length > 0" class="flex flex-col gap-12px">
          <RecommendCard
            v-for="item in list"
            :key="item.id"
            :item="item"
            :loading="actionLoading"
            @accept="handleAccept"
            @reject="handleReject"
          />
        </div>
        <NEmpty
          v-else-if="!loading"
          :title="$t('page.gateway.whitelistRecommend.emptyTitle')"
          :description="$t('page.gateway.whitelistRecommend.emptyDesc')"
        />
      </NSpin>
    </NCard>
  </div>
</template>

<style scoped>
.card-wrapper {
  flex: 1;
}
</style>
