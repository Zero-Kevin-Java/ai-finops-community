<script setup lang="ts">
import { watch } from 'vue';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';

defineOptions({
  name: 'PieChart'
});

interface PieDataItem {
  name: string;
  value: number;
}

const props = defineProps<{
  data?: PieDataItem[];
}>();

const appStore = useAppStore();

const { domRef, updateOptions } = useEcharts(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    bottom: '1%',
    left: 'center',
    itemStyle: {
      borderWidth: 0
    }
  },
  series: [
    {
      color: ['#5da8ff', '#8e9dff', '#fedc69', '#26deca', '#ff6b6b'],
      name: 'API Key 调用分布',
      type: 'pie',
      radius: ['45%', '75%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 1
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '12'
        }
      },
      labelLine: {
        show: false
      },
      data: [] as { name: string; value: number }[]
    }
  ]
}));

function updateData(newData: PieDataItem[]) {
  updateOptions(opts => {
    opts.series[0].data = newData.map(item => ({
      name: item.name,
      value: item.value
    }));
    return opts;
  });
}

function updateLocale() {
  updateOptions((opts, factory) => {
    const originOpts = factory();
    opts.series[0].name = originOpts.series[0].name;
    return opts;
  });
}

async function init() {
  if (props.data && props.data.length > 0) {
    updateData(props.data);
  }
}

watch(
  () => props.data,
  newData => {
    if (newData && newData.length > 0) {
      updateData(newData);
    }
  },
  { deep: true }
);

watch(
  () => appStore.locale,
  () => {
    updateLocale();
  }
);

// init
init();
</script>

<template>
  <NCard :bordered="false" class="card-wrapper">
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </NCard>
</template>

<style scoped></style>
