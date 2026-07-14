<script setup lang="ts">
import { computed } from 'vue';
import { createReusableTemplate } from '@vueuse/core';
import { useThemeStore } from '@/store/modules/theme';

defineOptions({
  name: 'CardData'
});

const props = defineProps<{
  totalRequests?: number;
  totalCost?: string;
}>();

interface CardData {
  key: string;
  title: string;
  value: number | string;
  unit: string;
  color: {
    start: string;
    end: string;
  };
  icon: string;
}

const cardData = computed<CardData[]>(() => [
  {
    key: 'requestCount',
    title: '过去7天请求数',
    value: props.totalRequests ?? 0,
    unit: '',
    color: {
      start: '#ec4786',
      end: '#b955a4'
    },
    icon: 'ant-design:bar-chart-outlined'
  },
  {
    key: 'totalCost',
    title: '过去7天总成本',
    value: props.totalCost ?? '0.00',
    unit: '¥',
    color: {
      start: '#865ec0',
      end: '#5144b4'
    },
    icon: 'ant-design:money-collect-outlined'
  },
  {
    key: 'modelCount',
    title: '活跃模型数',
    value: 3,
    unit: '',
    color: {
      start: '#56cdf3',
      end: '#719de3'
    },
    icon: 'carbon:model'
  },
  {
    key: 'avgLatency',
    title: '平均延迟',
    value: 45,
    unit: 'ms',
    color: {
      start: '#fcbc25',
      end: '#f68057'
    },
    icon: 'carbon:timer'
  }
]);

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();

const themeStore = useThemeStore();

function getGradientColor(color: CardData['color']) {
  return `linear-gradient(to bottom right, ${color.start}, ${color.end})`;
}
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <!-- define component start: GradientBg -->
    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div
        class="px-16px pb-4px pt-8px text-white"
        :style="{ backgroundImage: gradientColor, borderRadius: themeStore.themeRadius + 'px' }"
      >
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>
    <!-- define component end: GradientBg -->

    <NGrid cols="s:1 m:2 l:4" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in cardData" :key="item.key">
        <GradientBg :gradient-color="getGradientColor(item.color)" class="flex-1">
          <h3 class="text-16px">{{ item.title }}</h3>
          <div class="flex justify-between pt-12px">
            <SvgIcon :icon="item.icon" class="text-32px" />
            <CountTo
              :prefix="item.unit"
              :start-value="0"
              :end-value="typeof item.value === 'number' ? item.value : parseFloat(item.value) || 0"
              class="text-30px text-white dark:text-dark"
            />
          </div>
        </GradientBg>
      </NGi>
    </NGrid>
  </NCard>
</template>

<style scoped></style>
