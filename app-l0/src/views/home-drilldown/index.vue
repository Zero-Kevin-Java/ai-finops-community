<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { ECOption } from '@/hooks/common/echarts';
import { useEcharts } from '@/hooks/common/echarts';

defineOptions({ name: 'HomeDrilldown' });

type DrillType = 'kpi' | 'rank' | 'token' | 'chart' | 'opportunity' | 'list';
type SegmentKey = '部门' | '项目' | '应用' | '模型';

interface MetricCard {
  label: string;
  value: string;
  trend: string;
  tone: 'blue' | 'green' | 'orange' | 'purple';
}

interface DetailRow {
  name: string;
  owner: string;
  amount: string;
  requests: string;
  token: string;
  saving: string;
  status: string;
  statusClass: string;
  bar: number;
}

const route = useRoute();
const router = useRouter();

const pageLoading = ref(true);
const dataLoading = ref(false);
const selectedSegment = ref<SegmentKey>('部门');
const selectedMetric = ref('成本');

const segmentTabs: SegmentKey[] = ['部门', '项目', '应用', '模型'];
const metricTabs = ['成本', 'Token', '节省率'];
const chartLabels = Array.from({ length: 14 }, (_, index) => `05-${String(index + 18).padStart(2, '0')}`);

const drillType = computed(() => String(route.query.drill || 'list') as DrillType);
const pageTitle = computed(() => String(route.query.title || '成本治理下钻仪表盘'));
const pageSubtitle = computed(() => String(route.query.subtitle || '模拟范围 / 全部条件'));
const rangeLabel = computed(() => String(route.query.range || '本月'));
const currencyLabel = computed(() => String(route.query.currency || 'CNY'));
const tabLabel = computed(() => String(route.query.tab || '按部门/团队'));

const titleSeed = computed(() => {
  const raw = `${drillType.value}-${pageTitle.value}-${pageSubtitle.value}-${selectedSegment.value}-${selectedMetric.value}`;

  return Array.from(raw).reduce((sum, char) => sum + char.charCodeAt(0), 0);
});

const scenarioTitle = computed(() => {
  const labels: Record<DrillType, string> = {
    kpi: '核心指标明细',
    rank: '成本归因明细',
    token: 'Token结构明细',
    chart: '趋势波动明细',
    opportunity: '优化机会明细',
    list: '全量模拟明细'
  };

  return labels[drillType.value] || labels.list;
});

const metricCards = computed<MetricCard[]>(() => {
  const seed = titleSeed.value % 17;

  return [
    {
      label: '下钻总金额',
      value: moneyValue(42680 + seed * 930),
      trend: `+${(8.4 + seed / 10).toFixed(1)}%`,
      tone: 'blue'
    },
    {
      label: '模拟请求数',
      value: `${(324 + seed * 11).toLocaleString()} 万`,
      trend: `+${(5.6 + seed / 8).toFixed(1)}%`,
      tone: 'green'
    },
    {
      label: 'Token消耗',
      value: `${(812.6 + seed * 13.4).toFixed(1)} B`,
      trend: `+${(6.2 + seed / 12).toFixed(1)}%`,
      tone: 'purple'
    },
    {
      label: '可节省空间',
      value: moneyValue(8560 + seed * 420),
      trend: `${(18.7 + seed / 7).toFixed(1)}%`,
      tone: 'orange'
    }
  ];
});

const detailRows = computed<DetailRow[]>(() => {
  const seed = titleSeed.value % 23;
  const names: Record<SegmentKey, string[]> = {
    部门: ['客服组', '研发组', '销售组', '风控组', '数据组', '增长组'],
    项目: ['智能客服升级', '知识问答', '风控评估', '市场分析', '代码助手', '运营洞察'],
    应用: ['客服助手', '知识问答', '代码助手', '营销助手', '风控引擎', '数据分析'],
    模型: ['GPT-4o', 'GPT-4.1 mini', 'Claude 3.5', 'Qwen Max', 'DeepSeek R1', 'Gemini Pro']
  };

  return names[selectedSegment.value].map((name, index) => {
    const base = 42680 - index * 4380 + seed * 210;

    return {
      name,
      owner: ['张三', '李四', '王五', '赵六', '钱七', '周九'][index],
      amount: moneyValue(base),
      requests: `${(3240112 - index * 312840 + seed * 1700).toLocaleString()}`,
      token: `${(812.6 - index * 74.2 + seed * 2.1).toFixed(1)}`,
      saving: moneyValue(8560 - index * 620 + seed * 80),
      status: index % 3 === 0 ? '待处理' : index % 3 === 1 ? '建议采纳' : '观察中',
      statusClass: index % 3 === 0 ? 'warn' : index % 3 === 1 ? 'success' : 'info',
      bar: Math.max(26, 92 - index * 10 + (seed % 6))
    };
  });
});

const insightList = computed(() => [
  `当前${scenarioTitle.value}的主要波动来自${detailRows.value[0]?.name || '核心维度'}，建议优先拆分高峰时段与模型调用策略。`,
  `${rangeLabel.value}维度下，${selectedSegment.value}间差异明显，缓存命中和Prompt压缩可以直接影响节省空间。`,
  `返回首页后会重新触发图表刷新，避免布局恢复过程中 ECharts 容器宽度为 0。`
]);

function moneyValue(value: number) {
  return `¥ ${Math.max(0, Math.round(value)).toLocaleString()}`;
}

function buildTrendData(offset = 0) {
  const seed = titleSeed.value % 19;

  return chartLabels.map((_, index) => Math.round(24 + seed * 1.2 + Math.sin(index / 1.7) * 8 + index * 1.4 + offset));
}

function createTrendOptions(): ECOption {
  return {
    color: ['#2f67f6', '#25a966'],
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 24, top: 32, bottom: 34 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartLabels,
      axisLine: { lineStyle: { color: '#d7dfeb' } },
      axisTick: { show: false },
      axisLabel: { color: '#667085' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf1f6' } },
      axisLabel: { color: '#667085' }
    },
    series: [
      {
        name: selectedMetric.value,
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: buildTrendData(16),
        areaStyle: { color: 'rgba(47, 103, 246, 0.12)' },
        lineStyle: { width: 3 }
      },
      {
        name: '基线',
        type: 'line',
        smooth: true,
        symbolSize: 0,
        data: buildTrendData(0),
        lineStyle: { width: 2, type: 'dashed' }
      }
    ]
  };
}

function createMixOptions(): ECOption {
  const seed = titleSeed.value % 11;

  return {
    color: ['#2f67f6', '#39b772', '#f6a72a', '#8b5cf6'],
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: '#475467', fontSize: 12 }
    },
    series: [
      {
        name: selectedSegment.value,
        type: 'pie',
        radius: ['46%', '72%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{d}%', color: '#1f2937' },
        labelLine: { length: 12, length2: 8 },
        data: [
          { name: '输入消耗', value: 48 + seed },
          { name: '输出消耗', value: 26 + seed / 2 },
          { name: '缓存命中', value: 14 - seed / 3 },
          { name: '推理成本', value: 10 + seed / 4 }
        ]
      }
    ]
  };
}

const { domRef: trendRef, chart: trendChart, updateOptions: updateTrendOptions } = useEcharts(createTrendOptions);
const { domRef: mixRef, chart: mixChart, updateOptions: updateMixOptions } = useEcharts(createMixOptions);

async function refreshCharts() {
  await nextTick();
  await Promise.all([updateTrendOptions(() => createTrendOptions()), updateMixOptions(() => createMixOptions())]);
  trendChart.value?.resize();
  mixChart.value?.resize();
}

function goHome() {
  router.push({ path: '/home' });
}

function switchSegment(segment: SegmentKey) {
  if (selectedSegment.value === segment) return;
  selectedSegment.value = segment;
}

function switchMetric(metric: string) {
  if (selectedMetric.value === metric) return;
  selectedMetric.value = metric;
}

function focusRow(row: DetailRow) {
  dataLoading.value = true;
  window.setTimeout(() => {
    dataLoading.value = false;
    window.setTimeout(refreshCharts, 80);
  }, 360 + row.bar);
}

function refreshMockData() {
  dataLoading.value = true;
  selectedMetric.value = metricTabs[(metricTabs.indexOf(selectedMetric.value) + 1) % metricTabs.length];
  window.setTimeout(() => {
    dataLoading.value = false;
  }, 480);
}

watch([selectedSegment, selectedMetric, () => route.query], async () => {
  dataLoading.value = true;
  await nextTick();
  await refreshCharts();
  window.setTimeout(() => {
    dataLoading.value = false;
  }, 360);
});

onMounted(() => {
  window.setTimeout(async () => {
    pageLoading.value = false;
    await nextTick();
    await refreshCharts();
    window.setTimeout(refreshCharts, 120);
  }, 520);
});
</script>

<template>
  <main class="drill-page">
    <div v-if="pageLoading" class="drill-skeleton" aria-label="下钻页加载中">
      <div class="skeleton-line title"></div>
      <div class="skeleton-grid cards">
        <div v-for="item in 4" :key="item" class="skeleton-card"></div>
      </div>
      <div class="skeleton-grid body">
        <div class="skeleton-panel"></div>
        <div class="skeleton-panel"></div>
      </div>
    </div>

    <NSpin v-else :show="dataLoading" class="drill-spin">
      <template #description>正在切换模拟数据</template>

      <section class="drill-hero">
        <button class="back-button" @click="goHome">
          <SvgIcon icon="material-symbols:arrow-back-rounded" />
          返回首页
        </button>
        <div>
          <p>{{ scenarioTitle }}</p>
          <h1>{{ pageTitle }}</h1>
          <span>{{ pageSubtitle }} / {{ rangeLabel }} / {{ currencyLabel }} / {{ tabLabel }}</span>
        </div>
        <button class="refresh-button" @click="refreshMockData">
          <SvgIcon icon="material-symbols:refresh-rounded" />
          刷新模拟
        </button>
      </section>

      <section class="drill-toolbar" aria-label="下钻筛选">
        <div class="segmented">
          <button
            v-for="segment in segmentTabs"
            :key="segment"
            :class="{ active: selectedSegment === segment }"
            @click="switchSegment(segment)"
          >
            {{ segment }}
          </button>
        </div>
        <div class="segmented metric">
          <button
            v-for="metric in metricTabs"
            :key="metric"
            :class="{ active: selectedMetric === metric }"
            @click="switchMetric(metric)"
          >
            {{ metric }}
          </button>
        </div>
      </section>

      <section class="metric-grid" aria-label="下钻关键指标">
        <article
          v-for="card in metricCards"
          :key="card.label"
          class="metric-card"
          :class="[card.tone]"
          @click="refreshMockData"
        >
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <em>{{ card.trend }}</em>
        </article>
      </section>

      <section class="drill-grid">
        <article class="panel chart-panel">
          <header>
            <div>
              <h2>{{ selectedMetric }}趋势</h2>
              <span>{{ selectedSegment }}维度模拟波动</span>
            </div>
            <button @click="refreshMockData">重算</button>
          </header>
          <div ref="trendRef" class="trend-chart" role="img" aria-label="下钻趋势图"></div>
        </article>

        <article class="panel chart-panel">
          <header>
            <div>
              <h2>{{ selectedSegment }}结构</h2>
              <span>按 Token 与成本混合占比</span>
            </div>
            <button @click="refreshMockData">换一组</button>
          </header>
          <div ref="mixRef" class="mix-chart" role="img" aria-label="下钻结构图"></div>
        </article>
      </section>

      <section class="content-grid">
        <article class="panel table-panel">
          <header>
            <h2>明细数据</h2>
            <button @click="refreshMockData">生成数据</button>
          </header>
          <div class="detail-table">
            <div class="detail-row detail-head">
              <span>{{ selectedSegment }}</span>
              <span>负责人</span>
              <span>金额</span>
              <span>请求数</span>
              <span>Token(B)</span>
              <span>可节省</span>
              <span>状态</span>
            </div>
            <button v-for="row in detailRows" :key="row.name" class="detail-row" @click="focusRow(row)">
              <span class="name-cell">
                <b>{{ row.name }}</b>
                <i><em :style="{ width: `${row.bar}%` }"></em></i>
              </span>
              <span>{{ row.owner }}</span>
              <span>{{ row.amount }}</span>
              <span>{{ row.requests }}</span>
              <span>{{ row.token }}</span>
              <span>{{ row.saving }}</span>
              <span>
                <strong class="status-pill" :class="[row.statusClass]">{{ row.status }}</strong>
              </span>
            </button>
          </div>
        </article>

        <aside class="panel insight-panel">
          <header>
            <h2>优化判断</h2>
            <button @click="refreshMockData">更新</button>
          </header>
          <ul>
            <li v-for="item in insightList" :key="item">{{ item }}</li>
          </ul>
          <div class="action-row">
            <button @click="goHome">
              <SvgIcon icon="material-symbols:home-rounded" />
              首页总览
            </button>
            <button @click="refreshMockData">
              <SvgIcon icon="material-symbols:auto-graph-rounded" />
              重新模拟
            </button>
          </div>
        </aside>
      </section>
    </NSpin>
  </main>
</template>

<style scoped>
.drill-page {
  min-height: 100%;
  padding: 0 0 22px;
  color: #111827;
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  background:
    radial-gradient(circle at 12% 4%, rgba(47, 103, 246, 0.06), transparent 24%),
    linear-gradient(180deg, #fbfcff 0%, #f7f9fc 100%);
}

button {
  font: inherit;
  cursor: pointer;
}

.drill-spin {
  display: block;
  animation: page-enter 0.34s ease both;
}

.drill-skeleton {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-top: 6px;
}

.skeleton-line,
.skeleton-card,
.skeleton-panel {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: #eef3fb;
}

.skeleton-line::after,
.skeleton-card::after,
.skeleton-panel::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.74) 45%, transparent 100%);
  animation: skeleton-sweep 1.2s ease-in-out infinite;
  content: '';
  transform: translateX(-100%);
}

.skeleton-line.title {
  width: 320px;
  height: 44px;
}

.skeleton-grid {
  display: grid;
  gap: 18px;
}

.skeleton-grid.cards {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.skeleton-grid.body {
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.85fr);
}

.skeleton-card {
  height: 126px;
}

.skeleton-panel {
  height: 330px;
}

.drill-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 22px;
  align-items: center;
  min-height: 86px;
  padding: 4px 0 18px;
  animation: section-rise 0.42s ease both;
}

.drill-hero p,
.drill-hero h1 {
  margin: 0;
}

.drill-hero p {
  color: #2f67f6;
  font-size: 13px;
  font-weight: 700;
}

.drill-hero h1 {
  margin-top: 4px;
  color: #0b1220;
  font-size: 24px;
  font-weight: 800;
  line-height: 32px;
}

.drill-hero span {
  display: block;
  margin-top: 5px;
  color: #667085;
  font-size: 14px;
}

.back-button,
.refresh-button,
.chart-panel header button,
.table-panel header button,
.insight-panel header button,
.action-row button {
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid #d9e1ee;
  border-radius: 7px;
  background: #fff;
  color: #1f2937;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    background-color 0.18s ease;
}

.back-button {
  padding: 0 13px;
}

.refresh-button {
  padding: 0 15px;
  color: #fff;
  border-color: #2f67f6;
  background: linear-gradient(180deg, #3d74ff, #1f5ceb);
}

.back-button:hover,
.chart-panel header button:hover,
.table-panel header button:hover,
.insight-panel header button:hover,
.action-row button:hover {
  border-color: rgba(47, 103, 246, 0.45);
  box-shadow: 0 8px 20px rgba(47, 103, 246, 0.12);
  transform: translateY(-1px);
}

.refresh-button:hover {
  box-shadow: 0 9px 22px rgba(47, 103, 246, 0.2);
  transform: translateY(-1px);
}

.drill-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  animation: section-rise 0.42s 0.04s ease both;
}

.segmented {
  display: inline-grid;
  grid-auto-flow: column;
  overflow: hidden;
  border: 1px solid #d9e1ee;
  border-radius: 7px;
  background: #fff;
}

.segmented button {
  min-width: 78px;
  height: 36px;
  border: 0;
  border-left: 1px solid #d9e1ee;
  background: transparent;
  color: #111827;
}

.segmented button:first-child {
  border-left: 0;
}

.segmented .active {
  color: #fff;
  font-weight: 700;
  background: linear-gradient(180deg, #3d74ff, #1f5ceb);
}

.segmented.metric .active {
  background: linear-gradient(180deg, #22b875, #139d5f);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.metric-card,
.panel {
  border: 1px solid #e1e7f0;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 7px 20px rgba(16, 24, 40, 0.045);
}

.metric-card {
  min-height: 116px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 18px 22px;
  animation: section-rise 0.42s ease both;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.metric-card:nth-child(1) {
  animation-delay: 0.06s;
}

.metric-card:nth-child(2) {
  animation-delay: 0.1s;
}

.metric-card:nth-child(3) {
  animation-delay: 0.14s;
}

.metric-card:nth-child(4) {
  animation-delay: 0.18s;
}

.metric-card:hover {
  border-color: rgba(47, 103, 246, 0.34);
  box-shadow: 0 10px 26px rgba(47, 103, 246, 0.12);
  transform: translateY(-2px);
}

.metric-card span {
  color: #667085;
  font-size: 14px;
}

.metric-card strong {
  margin-top: 8px;
  color: #080d1c;
  font-size: 30px;
  font-weight: 800;
  line-height: 36px;
}

.metric-card em {
  margin-top: 6px;
  font-style: normal;
  font-size: 13px;
  font-weight: 700;
}

.metric-card.blue em {
  color: #2f67f6;
}

.metric-card.green em {
  color: #13a563;
}

.metric-card.orange em {
  color: #f59e0b;
}

.metric-card.purple em {
  color: #7c3aed;
}

.drill-grid,
.content-grid {
  display: grid;
  gap: 18px;
}

.drill-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  margin-bottom: 18px;
}

.content-grid {
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.55fr);
}

.chart-panel,
.table-panel,
.insight-panel {
  animation: section-rise 0.44s 0.16s ease both;
}

.chart-panel {
  min-height: 330px;
  padding: 18px 20px 14px;
}

.chart-panel header,
.table-panel header,
.insight-panel header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.chart-panel h2,
.table-panel h2,
.insight-panel h2 {
  margin: 0;
  color: #111827;
  font-size: 17px;
  font-weight: 800;
  line-height: 24px;
}

.chart-panel header span {
  display: block;
  margin-top: 3px;
  color: #667085;
  font-size: 13px;
}

.chart-panel header button,
.table-panel header button,
.insight-panel header button {
  min-width: 72px;
  padding: 0 12px;
}

.trend-chart,
.mix-chart {
  width: 100%;
  height: 260px;
}

.table-panel {
  min-height: 350px;
  padding: 18px 20px;
}

.detail-table {
  margin-top: 14px;
}

.detail-row {
  width: 100%;
  min-height: 46px;
  display: grid;
  grid-template-columns: 1.25fr 0.7fr 0.8fr 1fr 0.82fr 0.82fr 0.72fr;
  align-items: center;
  gap: 14px;
  border: 0;
  border-bottom: 1px solid #edf1f6;
  background: transparent;
  color: #1f2937;
  font-size: 14px;
  text-align: left;
  transition:
    background-color 0.18s ease,
    transform 0.18s ease;
}

.detail-row:not(.detail-head):hover {
  background: rgba(47, 103, 246, 0.045);
  transform: translateX(2px);
}

.detail-head {
  min-height: 38px;
  color: #475467;
  font-size: 13px;
  font-weight: 700;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.name-cell b {
  width: 96px;
  overflow: hidden;
  color: #111827;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name-cell i {
  width: 118px;
  height: 7px;
  overflow: hidden;
  border-radius: 99px;
  background: #e7ecf5;
}

.name-cell em {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2f67f6, #5d8dff);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 24px;
  border-radius: 5px;
  font-size: 12px;
}

.status-pill.warn {
  color: #ef4444;
  background: #fff0ed;
  border: 1px solid #ffc9bd;
}

.status-pill.success {
  color: #13a563;
  background: #eaf8ef;
  border: 1px solid #bce9ca;
}

.status-pill.info {
  color: #2f67f6;
  background: #edf4ff;
  border: 1px solid #c7d8ff;
}

.insight-panel {
  min-height: 350px;
  padding: 18px 20px;
}

.insight-panel ul {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.insight-panel li {
  position: relative;
  padding-left: 15px;
  color: #344054;
  font-size: 14px;
  line-height: 22px;
}

.insight-panel li::before {
  position: absolute;
  top: 9px;
  left: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2f67f6;
  content: '';
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 22px;
}

.action-row button {
  width: 100%;
}

@keyframes skeleton-sweep {
  to {
    transform: translateX(100%);
  }
}

@keyframes page-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes section-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid,
  .drill-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .drill-hero {
    grid-template-columns: 1fr;
  }

  .drill-toolbar {
    flex-direction: column;
  }

  .segmented {
    width: 100%;
    grid-auto-flow: row;
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .detail-row {
    grid-template-columns: 1.2fr 0.8fr 0.8fr 0.8fr;
  }

  .detail-row span:nth-child(4),
  .detail-row span:nth-child(5),
  .detail-row span:nth-child(6) {
    display: none;
  }
}
</style>
