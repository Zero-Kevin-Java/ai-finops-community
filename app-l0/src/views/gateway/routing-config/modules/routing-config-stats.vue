<script setup lang="ts">
import { computed } from 'vue';

defineOptions({
  name: 'RoutingConfigStats'
});

interface StatCard {
  key: keyof Api.Gateway.RoutingConfigStats;
  label: string;
  color: string;
  value?: number | null;
  suffix?: string;
  suffixClass?: string;
}

const props = defineProps<{
  stats: Api.Gateway.RoutingConfigStats | null;
  loading?: boolean;
}>();

const growthPrefix = computed(() => {
  const rate = Number(props.stats?.todayHitGrowthRate || 0);
  if (rate > 0) return '↑';
  if (rate < 0) return '↓';
  return '';
});

const growthClass = computed(() => {
  const rate = Number(props.stats?.todayHitGrowthRate || 0);
  if (rate > 0) return 'is-danger';
  if (rate < 0) return 'is-success';
  return '';
});

const cards = computed<StatCard[]>(() => [
  {
    key: 'totalRules',
    label: '规则总数',
    color: '#4f46e5',
    value: props.stats?.totalRules,
    suffix: props.stats && props.stats.todayNewRules > 0 ? `+${props.stats.todayNewRules}` : '',
    suffixClass: props.stats && props.stats.todayNewRules > 0 ? 'is-success' : ''
  },
  {
    key: 'enabledRules',
    label: '启用规则',
    color: '#10b981',
    value: props.stats?.enabledRules,
    suffix: props.stats ? `${formatRate(props.stats.enabledRate)}%` : ''
  },
  {
    key: 'recordOnlyRules',
    label: '仅记录规则',
    color: '#6d5dfc',
    value: props.stats?.recordOnlyRules
  },
  {
    key: 'todayHitCount',
    label: '今日命中次数',
    color: '#111827',
    value: props.stats?.todayHitCount,
    suffix: props.stats ? `${growthPrefix.value}${formatRate(props.stats.todayHitGrowthRate)}%` : '',
    suffixClass: growthClass.value
  },
  {
    key: 'fallbackHitCount',
    label: '兜底触发次数',
    color: '#f59e0b',
    value: props.stats?.fallbackHitCount
  },
  {
    key: 'denyHitCount',
    label: '拦截次数',
    color: '#dc2626',
    value: props.stats?.denyHitCount
  }
]);

function statValue(card: StatCard) {
  if (card.value === null || card.value === undefined) return null;
  return Number(card.value);
}

function formatRate(value?: number | null) {
  const numberValue = Number(value || 0);
  return Number.isInteger(numberValue) ? String(numberValue) : numberValue.toFixed(1);
}
</script>

<template>
  <div class="routing-stats-grid">
    <NCard v-for="card in cards" :key="card.key" :bordered="false" size="small" class="routing-stat-card">
      <NSkeleton v-if="loading && !stats" text :repeat="2" />
      <template v-else>
        <div class="routing-stat-label">{{ card.label }}</div>
        <div class="routing-stat-main">
          <NNumberAnimation
            v-if="statValue(card) !== null"
            :from="0"
            :to="Number(statValue(card))"
            :show-separator="true"
            :precision="0"
            class="routing-stat-number"
            :style="{ color: card.color }"
          />
          <span v-else class="routing-stat-empty">-</span>
          <span v-if="card.suffix" class="routing-stat-suffix" :class="card.suffixClass">
            {{ card.suffix }}
          </span>
        </div>
      </template>
    </NCard>
  </div>
</template>

<style scoped lang="scss">
.routing-stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.routing-stat-card {
  min-height: 88px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgb(15 23 42 / 6%);
}

.routing-stat-label {
  color: #667085;
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
}

.routing-stat-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 10px;
  min-width: 0;
}

.routing-stat-number {
  font-size: 24px;
  font-weight: 650;
  line-height: 1.1;
  letter-spacing: 0;
}

.routing-stat-empty {
  color: #8a99ad;
  font-size: 24px;
  font-weight: 650;
  line-height: 1.1;
}

.routing-stat-suffix {
  color: #8a99ad;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.routing-stat-suffix.is-success {
  color: #10b981;
}

.routing-stat-suffix.is-danger {
  color: #f43f5e;
}

@media (max-width: 1400px) {
  .routing-stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .routing-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
