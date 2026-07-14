<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui';
import type { ECOption } from '@/hooks/common/echarts';
import dayjs from 'dayjs';
import { computed, h, onMounted, ref, watch } from 'vue';
import { useEcharts } from '@/hooks/common/echarts';
import { fetchGetLlmHomeOverview } from '@/service/api/llm/home-overview';

defineOptions({ name: 'HomeIndex' });

type Tone = 'primary' | 'success' | 'warning' | 'danger' | 'purple' | 'muted';
type CurrencyUnit = 'CNY' | 'USD' | 'TOKEN';
type DatePresetValue = 'today' | 'last7' | 'month';
type TokenTabValue = '项目' | '应用' | '模型' | '员工';
type TokenDimension = 'project' | 'app' | 'model' | 'employee';
type IconTone = 'primary' | 'success' | 'warning' | 'danger' | 'purple' | 'cyan';

interface StatCard {
  label: string;
  value: string;
  tone: Tone;
  hint?: string;
  icon?: string;
  iconTone?: IconTone;
  featured?: boolean;
  trend?: boolean;
}

interface TokenRow {
  name: string;
  supplier?: string;
  department?: string;
  requestCount?: string;
  input: string;
  output: string;
  cache: string;
  reason: string;
  total: string;
  cny: string;
  usd: string;
}

interface TokenSummaryCard {
  label: string;
  title: string;
  value: string;
  ratio: string;
  icon: string;
  tone: 'purple' | 'blue' | 'orange' | 'red';
}

interface TokenLegend {
  label: string;
  value: string;
  ratio: string;
  color: string;
  chartValue: number;
}

interface QuotaRow {
  name: string;
  type: string;
  rate: number;
  remain: string;
  status: string;
  tone: Tone;
}

interface Shortcut {
  label: string;
  icon: string;
}

const activeTokenTab = ref<TokenTabValue>('项目');
const activeDatePreset = ref<DatePresetValue | null>('today');
const dateRange = ref<[number, number] | null>(getDatePresetRange('today'));
const lastSearchSnapshot = ref('');
const homeOverview = ref<Api.Llm.HomeOverview | null>(null);
const overviewLoading = ref(false);
let overviewRequestSeq = 0;

const searchFilters = ref({
  deptId: null as CommonType.IdType | null,
  projectId: null as CommonType.IdType | null,
  clientId: null as CommonType.IdType | null
});

const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: '美元', value: 'USD' },
  { label: 'Token', value: 'TOKEN' }
] satisfies Array<{ label: string; value: CurrencyUnit }>;

const datePresets = [
  { label: '今天', value: 'today' },
  { label: '近 7 天', value: 'last7' },
  { label: '本月', value: 'month' }
] satisfies Array<{ label: string; value: DatePresetValue }>;

const statCards = computed<StatCard[]>(() => {
  const stats = homeOverview.value?.moneyStats;
  const avgTokens = stats?.requestCount ? Math.round((stats.totalTokens || 0) / stats.requestCount) : 0;

  return [
    {
      label: '总消耗金额 (CNY)',
      value: formatCurrency(stats?.cnyTotalAmount, 'CNY'),
      hint: '当前筛选周期',
      tone: 'primary',
      icon: 'material-symbols:payments-outline-rounded',
      iconTone: 'primary',
      featured: true
    },
    {
      label: '总消耗金额 (USD)',
      value: formatCurrency(stats?.usdTotalAmount, 'USD'),
      hint: '当前筛选周期',
      tone: 'primary',
      icon: 'material-symbols:account-balance-wallet-outline-rounded',
      iconTone: 'primary',
      featured: true
    },
    {
      label: '总 TOKEN 消耗',
      value: formatInteger(stats?.totalTokens),
      icon: 'material-symbols:data-object-rounded',
      iconTone: 'primary',
      tone: 'purple'
    },
    {
      label: '平均单次TOKEN',
      value: formatInteger(avgTokens),
      icon: 'material-symbols:token-rounded',
      iconTone: 'success',
      tone: 'success'
    },
    {
      label: '路由命中率',
      value: formatPercent(stats?.routingHitRate),
      icon: 'material-symbols:account-tree-rounded',
      iconTone: 'primary',
      tone: 'purple'
    }
  ];
});

const tokenTabs = ['项目', '应用', '模型', '员工'] satisfies TokenTabValue[];

const tabDimensionMap: Record<TokenTabValue, TokenDimension> = {
  项目: 'project',
  应用: 'app',
  模型: 'model',
  员工: 'employee'
};

const dimensionLabelMap: Record<TokenDimension, string> = {
  project: '消耗最高项目',
  app: '消耗最高应用',
  model: '主力使用模型',
  employee: '消耗最高的员工'
};

const dimensionToneMap: Record<TokenDimension, TokenSummaryCard['tone']> = {
  project: 'purple',
  app: 'blue',
  model: 'orange',
  employee: 'red'
};

const dimensionIconMap: Record<TokenDimension, string> = {
  project: 'material-symbols:rocket-launch-rounded',
  app: 'material-symbols:apps-rounded',
  model: 'material-symbols:deployed-code',
  employee: 'material-symbols:person-alert-rounded'
};

const tokenSummaryCards = computed<TokenSummaryCard[]>(() => {
  const summaries = homeOverview.value?.tokenSummaries ?? [];
  return (['project', 'app', 'model', 'employee'] satisfies TokenDimension[]).map(dimension => {
    const item = summaries.find(summary => summary.dimension === dimension);
    return {
      label: dimensionLabelMap[dimension],
      title: item?.title || '暂无数据',
      value: formatInteger(item?.totalTokens),
      ratio: `占比 ${formatPercent(item?.ratio)}`,
      icon: dimensionIconMap[dimension],
      tone: dimensionToneMap[dimension]
    };
  });
});

const activeTokenDimension = computed<TokenDimension>(() => tabDimensionMap[activeTokenTab.value]);

const tokenRows = computed<TokenRow[]>(() => {
  const rows = homeOverview.value?.tokenRows?.[activeTokenDimension.value] ?? [];
  return rows.map(row => ({
    name: row.name || '暂无数据',
    supplier: activeTokenTab.value === '模型' ? row.supplier || '-' : undefined,
    department: activeTokenTab.value === '员工' ? row.department || '-' : undefined,
    requestCount: activeTokenTab.value === '员工' ? formatInteger(row.requestCount) : undefined,
    input: formatInteger(row.inputTokens),
    output: formatInteger(row.outputTokens),
    cache: formatInteger(row.cachedTokens),
    reason: formatInteger(row.reasoningTokens),
    total: formatInteger(row.totalTokens),
    cny: formatCurrency(row.cnyAmount, 'CNY'),
    usd: formatCurrency(row.usdAmount, 'USD')
  }));
});

const tokenLegendColors = ['#6f39db', '#16bf86', '#f59a0a', '#ef476f', '#2f80ed'];

const tokenLegend = computed<TokenLegend[]>(() => {
  const rows = homeOverview.value?.tokenRows?.[activeTokenDimension.value] ?? [];
  const total = rows.reduce((sum, row) => sum + Number(row.totalTokens || 0), 0);
  return rows.slice(0, 5).map((row, index) => {
    const chartValue = total > 0 ? Math.round((Number(row.totalTokens || 0) / total) * 100) : 0;
    return {
      label: row.name || `${activeTokenTab.value}${index + 1}`,
      value: `${chartValue}%`,
      ratio: `${chartValue}%`,
      color: tokenLegendColors[index] ?? '#94a3b8',
      chartValue
    };
  });
});

const tokenTableColumns = computed<DataTableColumns<TokenRow>>(() => {
  const metricColumns: DataTableColumns<TokenRow> = [
    { key: 'input', title: '输入\nToken', align: 'center', minWidth: 96 },
    { key: 'output', title: '输出\nToken', align: 'center', minWidth: 96 },
    { key: 'total', title: '总计消耗\nToken', align: 'center', minWidth: 112 },
    {
      key: 'amount',
      title: '总计消耗\n金额',
      align: 'center',
      minWidth: 144,
      render: row =>
        h('div', { class: 'token-amount-cell' }, [
          h('span', { class: 'is-cny' }, row.cny),
          h('span', { class: 'is-usd' }, row.usd)
        ])
    }
  ];

  const columns: DataTableColumns<TokenRow> = [
    { key: 'name', title: `${activeTokenTab.value}名称`, align: 'center', minWidth: 140, ellipsis: { tooltip: true } }
  ];

  if (activeTokenTab.value === '模型') {
    columns.push({ key: 'supplier', title: '供应商', align: 'center', minWidth: 96, ellipsis: { tooltip: true } });
  }

  if (activeTokenTab.value === '员工') {
    columns[0] = { key: 'name', title: '员工姓名', align: 'center', minWidth: 110, ellipsis: { tooltip: true } };
    columns.push(
      { key: 'department', title: '所属部门', align: 'center', minWidth: 100, ellipsis: { tooltip: true } },
      { key: 'requestCount', title: '请求总次数', align: 'center', minWidth: 112 }
    );
  }

  return [...columns, ...metricColumns];
});

const tokenTablePagination = computed(() => ({
  page: 1,
  pageSize: 3,
  itemCount: tokenRows.value.length,
  prefix: () => `共 ${tokenRows.value.length} 条记录`
}));

const tokenTableScrollX = computed(() => {
  if (activeTokenTab.value === '模型') return 688;
  if (activeTokenTab.value === '员工') return 772;
  return 592;
});

const tokenChartTotal = computed(() => {
  const rows = homeOverview.value?.tokenRows?.[activeTokenDimension.value] ?? [];
  return rows.reduce((sum, row) => sum + Number(row.totalTokens || 0), 0);
});

function createTokenChartOptions(): ECOption {
  const legendData =
    tokenLegend.value.length > 0
      ? tokenLegend.value
      : [{ label: '暂无数据', value: '0%', ratio: '0%', color: '#eef2f7', chartValue: 1 }];

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {d}%'
    },
    series: [
      {
        type: 'pie',
        radius: ['62%', '86%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        silent: false,
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 0
        },
        emphasis: {
          scale: true,
          scaleSize: 4
        },
        data: [
          ...legendData.map(item => ({
            name: item.label,
            value: item.chartValue,
            itemStyle: {
              color: item.color
            }
          })),
          {
            name: '其他',
            value: 10,
            itemStyle: {
              color: '#eef2f7'
            }
          }
        ]
      }
    ]
  };
}

const { domRef: tokenChartRef, updateOptions: updateTokenChartOptions } = useEcharts(createTokenChartOptions, {
  onRender: chart => chart.hideLoading(),
  onUpdated: chart => chart.hideLoading()
});

watch(activeTokenTab, () => {
  updateTokenChartOptions(() => createTokenChartOptions());
});

watch(homeOverview, () => {
  updateTokenChartOptions(() => createTokenChartOptions());
});

async function handleSearchFilterChange() {
  lastSearchSnapshot.value = JSON.stringify({
    activeDatePreset: activeDatePreset.value,
    dateRange: dateRange.value,
    deptId: searchFilters.value.deptId,
    projectId: searchFilters.value.projectId,
    clientId: searchFilters.value.clientId
  });

  await fetchHomeOverview();
}

async function fetchHomeOverview() {
  if (!dateRange.value) return;

  const requestSeq = ++overviewRequestSeq;
  overviewLoading.value = true;
  const { data, error } = await fetchGetLlmHomeOverview(buildHomeOverviewParams());
  if (requestSeq === overviewRequestSeq) {
    if (!error && data) {
      homeOverview.value = data;
    }
    overviewLoading.value = false;
  }
}

function buildHomeOverviewParams(): Api.Llm.HomeOverviewSearchParams {
  const range = dateRange.value ?? getDatePresetRange('today');
  return {
    beginTime: dayjs(range[0]).startOf('day').format('YYYY-MM-DD HH:mm:ss'),
    endTime: dayjs(range[1]).endOf('day').format('YYYY-MM-DD HH:mm:ss'),
    deptId: searchFilters.value.deptId,
    projectId: searchFilters.value.projectId,
    clientId: searchFilters.value.clientId
  };
}

function getDatePresetRange(value: DatePresetValue): [number, number] {
  const now = new Date();
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);

  const end = new Date(now);
  end.setHours(23, 59, 59, 999);

  if (value === 'last7') {
    start.setDate(start.getDate() - 6);
  }

  if (value === 'month') {
    start.setDate(1);
  }

  return [start.getTime(), end.getTime()];
}

function handleDatePresetChange(value: DatePresetValue) {
  activeDatePreset.value = value;
  dateRange.value = getDatePresetRange(value);
  handleSearchFilterChange();
}

function handleDateRangeChange(value: [number, number] | null) {
  dateRange.value = value;
  activeDatePreset.value = null;
  handleSearchFilterChange();
}

function handleProjectFilterChange() {
  searchFilters.value.clientId = null;
  handleSearchFilterChange();
}

function formatInteger(value: unknown) {
  const numberValue = Number(value || 0);
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(
    Number.isFinite(numberValue) ? numberValue : 0
  );
}

function formatDecimal(value: unknown, fractionDigits = 2) {
  const numberValue = Number(value || 0);
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits
  }).format(Number.isFinite(numberValue) ? numberValue : 0);
}

function formatCurrency(value: unknown, currency: 'CNY' | 'USD') {
  const prefix = currency === 'CNY' ? '¥' : '$';
  return `${prefix}${formatDecimal(value)}`;
}

function formatPercent(value: unknown) {
  const numberValue = Number(value || 0);
  return `${formatDecimal(Number.isFinite(numberValue) ? numberValue : 0)}%`;
}
</script>

<template>
  <main class="cost-home">
    <creativity-banner />

    <NCard :bordered="false" size="small" class="filter-bar card-wrapper" aria-label="筛选条件">
      <NForm :model="searchFilters" label-placement="left" :label-width="78" :show-feedback="false">
        <NGrid responsive="screen" item-responsive :x-gap="16" :y-gap="12">
          <NFormItemGi span="24 s:24 m:12 xl:8" label="统计周期" path="dateRange">
            <div class="date-filter">
              <NDatePicker
                v-model:value="dateRange"
                type="daterange"
                clearable
                class="date-range"
                format="yyyy/MM/dd"
                @update:value="handleDateRangeChange"
              />
              <NButtonGroup size="small" class="date-presets">
                <NButton
                  v-for="preset in datePresets"
                  :key="preset.value"
                  :type="activeDatePreset === preset.value ? 'primary' : 'default'"
                  :ghost="activeDatePreset !== preset.value"
                  @click="handleDatePresetChange(preset.value)"
                >
                  {{ preset.label }}
                </NButton>
              </NButtonGroup>
            </div>
          </NFormItemGi>
          <NFormItemGi span="24 s:12 m:6 xl:4" label="组织架构" path="deptId">
            <DeptTreeSelect
              v-model:value="searchFilters.deptId"
              clearable
              placeholder="全部组织"
              @update:value="handleSearchFilterChange"
            />
          </NFormItemGi>
          <NFormItemGi span="24 s:12 m:6 xl:4" label="项目" path="projectId">
            <LlmProjectSelect
              v-model:value="searchFilters.projectId"
              clearable
              placeholder="全部项目"
              @update:value="handleProjectFilterChange"
            />
          </NFormItemGi>
          <NFormItemGi span="24 s:12 m:6 xl:4" label="应用" path="clientId">
            <LlmAppClientSelect
              v-model:value="searchFilters.clientId"
              :project-id="searchFilters.projectId"
              clearable
              placeholder="全部应用"
              @update:value="handleSearchFilterChange"
            />
          </NFormItemGi>
        </NGrid>
      </NForm>
    </NCard>

    <section v-if="overviewLoading && !homeOverview" class="loading-strip">正在加载当前租户首页数据...</section>

    <section class="stat-card-grid" aria-label="统计指标">
      <article
        v-for="item in statCards"
        :key="item.label"
        class="stat-card"
        :class="[`tone-${item.tone}`, { 'is-featured': item.featured }]"
      >
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p v-if="item.hint" :class="{ 'is-trend': item.trend }">
            <SvgIcon v-if="item.trend" icon="material-symbols:trending-down-rounded" />
            {{ item.hint }}
          </p>
        </div>
        <span v-if="item.icon" class="stat-icon" :class="`icon-${item.iconTone ?? item.tone}`">
          <SvgIcon :icon="item.icon" class="stat-icon-symbol" />
        </span>
      </article>
    </section>

    <section class="panel token-panel">
      <header class="section-head token-head">
        <h2>TOKEN消耗分析</h2>
      </header>

      <div class="token-summary-grid">
        <article
          v-for="item in tokenSummaryCards"
          :key="item.label"
          class="token-summary-card"
          :class="`tone-${item.tone}`"
        >
          <div class="token-summary-meta">
            <span class="token-summary-icon">
              <SvgIcon :icon="item.icon" />
            </span>
            <span>{{ item.label }}</span>
          </div>
          <strong>{{ item.title }}</strong>
          <p>
            <b>{{ item.value }}</b>
            <span>{{ item.ratio }}</span>
          </p>
        </article>
      </div>

      <div class="token-tabs">
        <button
          v-for="tab in tokenTabs"
          :key="tab"
          :class="{ active: activeTokenTab === tab }"
          @click="activeTokenTab = tab"
        >
          {{ tab }}
        </button>
      </div>

      <div class="token-body" :class="{ 'is-table-only': activeTokenTab === '员工' }">
        <div v-if="activeTokenTab !== '员工'" class="donut-column">
          <div class="token-chart-wrap">
            <div ref="tokenChartRef" class="token-chart" role="img" aria-label="Token 消耗占比图表"></div>
            <div class="token-chart-center" aria-hidden="true">
              <span>总消耗</span>
              <strong>{{ formatInteger(tokenChartTotal) }}</strong>
            </div>
          </div>
          <ul>
            <li v-for="item in tokenLegend" :key="item.label">
              <i :style="{ backgroundColor: item.color }"></i>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </li>
          </ul>
        </div>

        <div class="token-table">
          <DataTable
            size="small"
            :columns="tokenTableColumns"
            :data="tokenRows"
            :pagination="tokenTablePagination"
            :row-key="row => row.name"
            :scroll-x="tokenTableScrollX"
            class="token-data-table"
          />
        </div>
      </div>
    </section>

    <div class="page-bottom-spacer" aria-hidden="true"></div>
  </main>
</template>

<style scoped>
.cost-home.cost-home {
  box-sizing: border-box;
  min-height: 100%;
  padding: 12px 16px 48px;
  background: #f5f7fb;
  color: #26334d;
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}

button {
  font: inherit;
}

.panel {
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(58, 76, 110, 0.06);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.filter-bar {
  border: 1px solid #dde5f0;
  border-radius: 8px;
  margin-top: 16px;
  box-shadow: 0 8px 20px rgba(58, 76, 110, 0.06);
  animation: homeFadeUp 0.42s ease backwards;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.filter-bar:hover {
  border-color: #cfd8e8;
  box-shadow: 0 10px 24px rgba(58, 76, 110, 0.08);
}

.filter-bar :deep(.n-card__content) {
  padding: 18px 22px 16px;
}

.filter-bar :deep(.n-form-item-label) {
  color: #6b778d;
  font-size: 13px;
  font-weight: 700;
}

.loading-strip {
  display: flex;
  min-height: 42px;
  align-items: center;
  margin-top: 16px;
  padding: 0 18px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(58, 76, 110, 0.05);
}

.date-filter {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.date-range {
  width: 100%;
}

.date-presets {
  flex-shrink: 0;
}

.stat-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px 20px;
  margin-top: 16px;
}

.stat-card {
  position: relative;
  display: flex;
  min-height: 96px;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 22px 26px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(58, 76, 110, 0.05);
  animation: homeFadeUp 0.42s ease backwards;
  isolation: isolate;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.stat-card:hover,
.route-card:hover,
.shortcut-grid button:hover {
  border-color: #cfd8e8;
  box-shadow: 0 12px 28px rgba(49, 65, 94, 0.1);
  transform: translateY(-2px);
}

.stat-card.is-featured {
  min-height: 136px;
  align-items: center;
  padding: 30px 28px 26px;
}

.stat-card.is-featured::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  content: '';
}

.stat-card.is-featured.tone-primary::before {
  background: #514eea;
}

.stat-card.is-featured.tone-success::before {
  background: #17c88e;
}

.stat-card span,
.saving-summary span,
.sync-card span,
.quota-summary > span {
  display: block;
  color: #9aa7b8;
  font-size: 14px;
  font-weight: 800;
  line-height: 20px;
}

.stat-card strong {
  display: block;
  margin-top: 12px;
  color: #1f2a3d;
  font-size: 26px;
  font-weight: 800;
  line-height: 32px;
  letter-spacing: 0;
  white-space: nowrap;
}

.stat-card.is-featured strong {
  margin-top: 18px;
  font-size: 34px;
  line-height: 40px;
}

.stat-card.tone-success strong,
.link {
  color: #16bd86;
}

.stat-card p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 24px 0 0;
  color: #9aa7b8;
  font-size: 13px;
  font-weight: 800;
}

.stat-card p.is-trend {
  color: #16bd86;
}

.stat-card p .svg-icon {
  font-size: 22px;
}

.stat-icon {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(111, 99, 232, 0.18);
  border-radius: 11px;
  background: #f0efff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 10px 18px rgba(86, 92, 232, 0.12);
  box-sizing: border-box;
  color: #6a5ff0;
  align-items: center;
  justify-content: center;
  line-height: 1;
  place-items: center;
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.stat-icon :deep(.stat-icon-symbol) {
  position: absolute;
  top: 50%;
  left: 50%;
  display: block;
  width: 22px;
  height: 22px;
  margin: 0;
  font-size: 22px;
  line-height: 1;
  transform: translate(-50%, -50%);
}

.stat-card:hover .stat-icon {
  transform: translateY(-2px) scale(1.04);
}

.stat-card.is-featured .stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
}

.stat-card.is-featured .stat-icon :deep(.stat-icon-symbol) {
  width: 24px;
  height: 24px;
  font-size: 24px;
}

.stat-icon.icon-primary {
  border-color: rgba(81, 78, 234, 0.18);
  background: #efefff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 12px 22px rgba(81, 78, 234, 0.13);
  color: #514eea;
}

.stat-icon.icon-success {
  border-color: rgba(22, 189, 134, 0.2);
  background: #eafbf5;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 12px 22px rgba(22, 189, 134, 0.13);
  color: #16bd86;
}

.stat-icon.icon-purple {
  border-color: rgba(111, 99, 232, 0.18);
  background: #f1efff;
  color: #7567ef;
}

.stat-icon.icon-warning {
  border-color: rgba(245, 158, 11, 0.22);
  background: #fff7e8;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 12px 22px rgba(245, 158, 11, 0.12);
  color: #d98200;
}

.stat-icon.icon-danger {
  border-color: rgba(239, 68, 68, 0.2);
  background: #fff0f0;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 12px 22px rgba(239, 68, 68, 0.1);
  color: #d73535;
}

.stat-icon.icon-cyan {
  border-color: rgba(14, 165, 233, 0.2);
  background: #eaf8ff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 12px 22px rgba(14, 165, 233, 0.1);
  color: #0284c7;
}

.section-head {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
  border-bottom: 1px solid #e7edf5;
}

.section-head h2,
.shadow-title h2 {
  position: relative;
  margin: 0;
  padding-left: 14px;
  color: #33405a;
  font-size: 16px;
  font-weight: 800;
  line-height: 22px;
}

.section-head h2::before,
.shadow-title h2::before {
  position: absolute;
  left: 0;
  top: 2px;
  width: 5px;
  height: 18px;
  border-radius: 999px;
  background: #565ce8;
  content: '';
}

.billing-panel,
.token-panel,



.billing-head h2 {
  padding-left: 16px;
  color: #26334d;
  font-size: 16px;
  line-height: 22px;
}

.billing-head h2::before {
  top: 2px;
  width: 5px;
  height: 18px;
  background: #5553ea;
}


.billing-link :deep(.n-button__content) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.billing-link-icon {
  font-size: 16px;
}

.billing-link :deep(.n-button__icon) {
  margin-left: 4px;
  font-size: 16px;
}

.token-panel {
  animation-delay: 0.24s;
}

.quota-panel {
  animation-delay: 0.3s;
}

.panel:hover {
  border-color: #cfd8e8;
  box-shadow: 0 10px 24px rgba(58, 76, 110, 0.08);
}


.saving-summary span {
  color: #6d7a92;
  font-size: 16px;
  font-weight: 700;
  line-height: 22px;
}

.saving-summary strong {
  display: block;
  margin-top: 20px;
  color: #5553ea;
  font-size: 42px;
  font-weight: 800;
  line-height: 48px;
  letter-spacing: 0;
}

.saving-summary em {
  display: inline-flex;
  align-items: center;
  margin-top: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #dff9ed;
  color: #12b981;
  font-size: 15px;
  font-style: normal;
  font-weight: 700;
  line-height: 18px;
  animation: pulseSoft 2.6s ease-in-out infinite;
}


.bill-compare-card::before {
  position: absolute;
  top: 62px;
  left: 44px;
  z-index: 0;
  width: 0;
  height: 52px;
  border-left: 2px dashed #c9cede;
  content: '';
}


.bill-line i {
  position: relative;
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border: 3px solid #7d8798;
  border-radius: 12px;
  background: transparent;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.bill-line i::after {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #647087;
  content: '';
}

.bill-line:hover i {
  transform: scale(1.08);
}

.bill-line.optimized i {
  border-color: #5553ea;
  background: #5553ea;
  box-shadow: 0 6px 12px rgba(85, 83, 234, 0.25);
}

.bill-line.optimized i::after {
  width: auto;
  height: auto;
  border-radius: 0;
  background: transparent;
  color: #fff;
  content: '✓';
  font-size: 15px;
  font-style: normal;
  font-weight: 800;
}

.bill-line span,
.sync-card span,
.sync-card p {
  color: #71809a;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
}

.bill-line strong {
  display: block;
  margin-top: 6px;
  color: #33405a;
  font-size: 20px;
  font-weight: 800;
  line-height: 24px;
  letter-spacing: 2px;
}

.bill-line.optimized span,
.bill-line.optimized strong {
  color: #5753e9;
}


.sync-card:hover {
  border-color: #cfd8e8;
  box-shadow: 0 10px 22px rgba(58, 76, 110, 0.08);
  transform: translateY(-1px);
}

.sync-card strong {
  display: block;
  margin-top: 24px;
  color: #39465f;
  font-size: 30px;
  font-weight: 800;
  line-height: 36px;
  letter-spacing: 1px;
}

.sync-card small {
  margin-left: 5px;
  color: #56647d;
  font-size: 16px;
  font-weight: 600;
}

.sync-card > .svg-icon {
  position: absolute;
  top: 24px;
  right: 28px;
  color: #ff9c00;
  font-size: 26px;
  transition: transform 0.2s ease;
}

.sync-card:hover > .svg-icon {
  transform: rotate(8deg) scale(1.08);
}

.token-head {
  min-height: 60px;
  padding: 0 24px;
  border-bottom-color: #dfe6f0;
}

.token-head h2 {
  padding-left: 15px;
  color: #1f2a3d;
  font-size: 18px;
  line-height: 24px;
}

.token-head h2::before {
  top: 1px;
  width: 5px;
  height: 22px;
  background: #565ce8;
}

.token-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #dfe6f0;
  background: #fff;
}

.token-summary-card {
  min-height: 130px;
  padding: 20px 20px 18px;
  border: 1px solid #dfe6f0;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(58, 76, 110, 0.05);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.token-summary-card:hover {
  border-color: #cfd8e8;
  box-shadow: 0 12px 24px rgba(58, 76, 110, 0.08);
  transform: translateY(-2px);
}

.token-summary-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.token-summary-icon {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-size: 23px;
}

.token-summary-card strong {
  display: block;
  margin-top: 14px;
  color: #1f2a3d;
  font-size: 21px;
  font-weight: 800;
  line-height: 28px;
}

.token-summary-card p {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 8px 0 0;
}

.token-summary-card b {
  font-size: 25px;
  font-weight: 800;
  line-height: 32px;
}

.token-summary-card p span {
  color: #748197;
  font-size: 12px;
  font-weight: 700;
}

.token-summary-card.tone-purple .token-summary-icon {
  background: #eee3ff;
  color: #6f39db;
}

.token-summary-card.tone-purple b {
  color: #6f39db;
}

.token-summary-card.tone-blue .token-summary-icon {
  background: #e5e4ff;
  color: #4d50dd;
}

.token-summary-card.tone-blue b {
  color: #4d50dd;
}

.token-summary-card.tone-orange .token-summary-icon {
  background: #ffe1c8;
  color: #a85608;
}

.token-summary-card.tone-orange b {
  color: #a85608;
}

.token-summary-card.tone-red .token-summary-icon {
  background: #ffe0df;
  color: #c42024;
}

.token-summary-card.tone-red b {
  color: #c42024;
}

.currency-tabs-card {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  width: fit-content;
  padding: 4px;
  border: 1px solid color-mix(in srgb, var(--n-text-color-3) 18%, var(--n-color));
  border-radius: 7px;
  background-color: #e2e8f0;
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 45%);
}

.currency-tabs-card button {
  min-width: 60px;
  height: 28px;
  padding: 0 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  line-height: 26px;
  transition:
    color 0.2s var(--n-bezier),
    background-color 0.2s var(--n-bezier),
    box-shadow 0.2s var(--n-bezier);
}

.currency-tabs-card button:hover,
.currency-tabs-card .is-active {
  color: rgb(var(--primary-color));
}

.currency-tabs-card .is-active {
  background: #fff;
  box-shadow: 0 1px 4px rgb(0 0 0 / 10%);
}

.token-tabs {
  display: flex;
  align-items: center;
  gap: 30px;
  min-height: 58px;
  padding: 0 24px;
  border-bottom: 1px solid #dfe6f0;
  background: #fff;
}

.token-tabs button {
  position: relative;
  height: 58px;
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 15px;
  font-weight: 800;
  transition: color 0.2s ease;
}

.token-tabs button.active {
  color: #565ce8;
}

.token-tabs button.active::after {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 0;
  background: #565ce8;
  content: '';
  animation: tabIndicatorIn 0.22s ease both;
}

.token-tabs button:hover {
  color: #565ce8;
}

.token-body {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  min-height: 400px;
  background: #fff;
}

.token-body.is-table-only {
  grid-template-columns: minmax(0, 1fr);
}

.donut-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 20px 24px;
  border-right: 0;
}

.token-chart-wrap {
  position: relative;
  width: 172px;
  height: 172px;
  margin: 0 auto 58px;
  animation: donutIn 0.72s ease both;
}

.token-chart {
  width: 100%;
  height: 100%;
}

.token-chart-center {
  position: absolute;
  inset: 35px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: #fff;
  pointer-events: none;
}

.token-chart-center strong {
  color: #1f2a3d;
  font-size: 21px;
  font-weight: 800;
  line-height: 26px;
}

.token-chart-center span {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.donut-column ul {
  display: grid;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.donut-column li {
  display: grid;
  grid-template-columns: 10px 52px 42px;
  align-items: center;
  gap: 8px;
  color: #1f2a3d;
  font-size: 14px;
  font-weight: 800;
}

.donut-column li i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transition: transform 0.2s ease;
}

.donut-column li:hover i {
  transform: scale(1.25);
}

.donut-column li strong,
.donut-column li em {
  color: #1f2a3d;
  font-style: normal;
  text-align: left;
}

.token-table {
  min-width: 0;
  padding: 0;
  overflow: hidden;
}

.token-data-table {
  height: 100%;
}

.token-data-table :deep(.n-data-table-th) {
  height: 68px;
  background: #f1f3f6;
  color: #607086;
  font-size: 15px;
  font-weight: 800;
  line-height: 20px;
  white-space: pre-line;
}

.token-data-table :deep(.n-data-table-td) {
  height: 54px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.token-data-table :deep(.token-amount-cell) {
  display: inline-grid;
  gap: 4px;
  justify-items: center;
  line-height: 18px;
}

.token-data-table :deep(.token-amount-cell span) {
  display: block;
  font-variant-numeric: tabular-nums;
}

.token-data-table :deep(.token-amount-cell .is-cny) {
  color: #16bd86;
}

.token-data-table :deep(.token-amount-cell .is-usd) {
  color: #64748b;
}

.token-data-table :deep(.n-data-table-tr:hover .n-data-table-td) {
  background: #f8faff;
}

.token-data-table :deep(.n-data-table-wrapper) {
  border-radius: 0;
}

.token-data-table :deep(.n-data-table__pagination) {
  min-height: 62px;
  align-items: center;
  margin: 0;
  padding: 0 24px;
  border-top: 1px solid #e7edf5;
}

.token-data-table :deep(.n-pagination-prefix) {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}



.quota-summary > strong {
  display: block;
  margin-top: 10px;
  color: #26334d;
  font-size: 24px;
  line-height: 30px;
  letter-spacing: 0;
}


.quota-badge small {
  color: #8390a5;
  font-size: 10px;
}


.quota-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #7649e8;
  animation: progressGrow 0.75s ease both;
  transform-origin: left center;
}


.quota-cards div {
  min-height: 72px;
  padding: 14px;
  border: 1px solid #e4eaf3;
  border-radius: 7px;
  background: #f4f7fb;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.quota-cards div:hover {
  border-color: #cfd8e8;
  transform: translateY(-1px);
}

.quota-cards div:last-child {
  background: #f3f2ff;
}

.quota-cards span {
  color: #8995aa;
  font-size: 12px;
  font-weight: 700;
}

.quota-cards strong {
  display: block;
  margin-top: 6px;
  color: #5e6a7f;
  font-size: 15px;
  font-weight: 800;
}

.quota-cards div:last-child strong {
  color: #565ce8;
}



.quota-table header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: #33405a;
  font-weight: 800;
}

.quota-table header span {
  color: #a0aabd;
  font-size: 12px;
  font-weight: 700;
}



.quota-row span {
  padding: 0 10px;
}


.rate-cell i {
  width: 70px;
  height: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5eaf2;
}

.rate-cell b {
  display: block;
  height: 100%;
  border-radius: inherit;
  animation: progressGrow 0.75s ease both;
  transform-origin: left center;
}

.rate-cell .tone-danger b {
  background: #ff4f5d;
}

.rate-cell .tone-warning b {
  background: #f7a000;
}

.rate-cell .tone-primary b {
  background: #7452e8;
}

.quota-row em {
  display: inline-flex;
  min-width: 42px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  font-style: normal;
  font-size: 12px;
}






.shadow-title h2 {
  padding-left: 14px;
  color: #1f2a3d;
  font-size: 15px;
  font-weight: 800;
  line-height: 22px;
}

.shadow-title h2::before {
  top: 1px;
  width: 5px;
  height: 20px;
  border-radius: 2px;
  background: #565ce8;
}


.shadow-compare::before {
  position: absolute;
  top: 14px;
  bottom: 28px;
  left: 50%;
  width: 1px;
  background: #eef2f7;
  content: '';
  transform: translateX(-50%);
}


.route-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 39px;
  margin: 0 -20px 14px;
  padding: 0 18px;
}

.route-card.normal header {
  background: #4f46dc;
  color: #fff;
}

.route-card.shadow {
  border-color: #bdebdc;
  box-shadow: 0 18px 28px rgba(32, 125, 93, 0.16);
}

.route-card.shadow header {
  border-top: 3px solid #18c592;
  background: #eefbf6;
  color: #02a86f;
}

.route-card h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
}

.route-card header button,
.route-card footer button {
  border: 0;
  border-radius: 3px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.22);
  color: inherit;
  font-size: 10px;
  font-weight: 800;
}

.route-card.shadow header button {
  display: none;
}

.route-card dl {
  display: grid;
  gap: 0;
  margin: 0;
}

.route-card dl div {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  min-height: 40px;
  align-items: center;
  border-bottom: 1px solid #e7edf5;
}

.route-card dt {
  color: #8490a4;
  font-size: 12px;
  font-weight: 700;
}

.route-card dd {
  margin: 0;
  color: #1f2a3d;
  font-size: 13px;
  font-weight: 800;
  text-align: right;
}

.route-card.shadow dd {
  color: #18b980;
}


.route-score span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #13bf83;
  animation: progressGrow 0.75s ease both;
  transform-origin: left center;
}


.route-costs span {
  display: grid;
  gap: 5px;
  min-height: 50px;
  padding: 10px 12px;
  border-radius: 4px;
  background: #edf0f4;
  color: #9aa5b7;
  font-size: 10px;
  font-weight: 800;
}

.route-card.shadow .route-costs span {
  border: 1px solid #ccefe3;
  background: #effbf7;
  color: #12b986;
}

.route-costs strong {
  color: #1f2a3d;
  font-size: 14px;
}

.route-card footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-top: 22px;
  color: #68758a;
  font-size: 12px;
  font-weight: 700;
}

.route-card.normal footer strong,
.route-card.shadow footer strong {
  color: #4f46dc;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 18px;
  font-weight: 800;
}

.route-card.shadow footer strong {
  display: block;
  color: #19b980;
  font-size: 20px;
}

.route-card.shadow footer em {
  display: block;
  color: #9aa5b7;
  font-size: 10px;
  font-style: normal;
  line-height: 14px;
  text-decoration: line-through;
}

.route-card.shadow footer button {
  background: #10b879;
  color: #fff;
}

.route-middle {
  position: relative;
  z-index: 2;
  display: grid;
  align-self: center;
  justify-items: center;
}

.route-middle b {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid #ccd6e5;
  border-radius: 50%;
  background: #fff;
  color: #5565d9;
  box-shadow: 0 6px 14px rgba(42, 58, 92, 0.14);
  font-size: 20px;
}

.advice-bar {
  min-height: 60px;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 12px 16px;
  border: 1px solid #ccd7ff;
  border-radius: 7px;
  background: #eef1ff;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.advice-bar:hover {
  border-color: #bfc9ff;
  box-shadow: 0 10px 24px rgba(86, 92, 232, 0.08);
}

.advice-bar > .svg-icon {
  width: 36px;
  height: 36px;
  padding: 9px;
  border-radius: 7px;
  background: #dfe4ff;
  color: #565ce8;
}

.advice-bar p {
  margin: 0;
  color: #738098;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.advice-bar strong {
  display: block;
  color: #33405a;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.page-bottom-spacer {
  height: 32px;
}

.shortcut-grid button {
  min-height: 60px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  border: 1px solid #e1e8f2;
  border-radius: 8px;
  background: #fff;
  color: #33405a;
  box-shadow: 0 8px 18px rgba(58, 76, 110, 0.06);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.shortcut-grid .svg-icon {
  font-size: 19px;
  transition: transform 0.2s ease;
}

.shortcut-grid button:hover {
  color: #565ce8;
}

.shortcut-grid button:hover .svg-icon {
  transform: translateY(-2px);
}

.shortcut-grid span {
  font-size: 13px;
  font-weight: 800;
}

@keyframes homeFadeUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes donutIn {
  from {
    opacity: 0;
    transform: rotate(-18deg) scale(0.94);
  }

  to {
    opacity: 1;
    transform: rotate(0) scale(1);
  }
}

@keyframes progressGrow {
  from {
    transform: scaleX(0);
  }

  to {
    transform: scaleX(1);
  }
}

@keyframes tabIndicatorIn {
  from {
    transform: scaleX(0.35);
  }

  to {
    transform: scaleX(1);
  }
}

@keyframes pulseSoft {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-1px);
  }
}

@keyframes routePulse {
  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 10px 18px rgba(86, 92, 232, 0.24);
  }

  50% {
    transform: scale(1.04);
    box-shadow: 0 12px 24px rgba(86, 92, 232, 0.3);
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}

@media (max-width: 1280px) {
  .stat-card-grid,
  .token-summary-grid,
  .shortcut-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .billing-body,
  .quota-body,
  .token-body,
  .shadow-compare {
    grid-template-columns: 1fr;
  }

  .donut-column,
  .quota-summary {
    border-right: 0;
    border-bottom: 1px solid #e7edf5;
  }

  .route-middle {
    display: none;
  }
}

@media (max-width: 768px) {
  .cost-home {
    padding-right: 14px;
    padding-left: 14px;
  }

  .filter-bar,
  .stat-card-grid,
  .token-summary-grid,
  .shortcut-grid,
  .quota-cards {
    grid-template-columns: 1fr;
  }

  .filter-bar :deep(.n-card__content) {
    padding: 16px;
  }

  .date-filter {
    grid-template-columns: 1fr;
  }

  .date-presets {
    width: 100%;
  }

  .date-presets :deep(.n-button) {
    flex: 1;
  }

  .section-head,
  .shadow-title,
  .advice-bar {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .token-head {
    flex-wrap: wrap;
  }

  .currency-tabs-card {
    width: 100%;
  }

  .currency-tabs-card button {
    flex: 1;
    min-width: 0;
  }

  .billing-body,
  .quota-table,
  .quota-summary,
  .donut-column {
    padding: 20px;
  }

  .token-summary-card {
    min-height: 118px;
  }

  .saving-summary strong {
    font-size: 28px;
  }

  .token-chart-wrap {
    width: 180px;
    height: 180px;
  }

  .quota-row {
    min-width: 620px;
  }

  .quota-table {
    overflow-x: auto;
  }
}
</style>
