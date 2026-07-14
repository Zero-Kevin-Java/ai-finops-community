<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { fetchGetLlmModelDetail } from '@/service/api/llm/model';

defineOptions({
  name: 'ModelDetailModal'
});

interface Props {
  modelId?: CommonType.IdType | null;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', {
  default: false
});

type DetailTab = 'basic' | 'usage';

const loading = ref(false);
const detail = ref<Api.Llm.ModelDetail | null>(null);
const activeTab = ref<DetailTab>('basic');

const model = computed(() => detail.value?.model);
const usageStats = computed(
  () =>
    detail.value?.usageStats || {
      promptTokens: 0,
      completionTokens: 0,
      totalTokens: 0,
      requestCount: 0,
      totalAmount: 0
    }
);

const tabs: Array<{ key: DetailTab; label: string }> = [
  { key: 'basic', label: '基础信息' },
  { key: 'usage', label: '使用统计' }
];

watch(
  () => [visible.value, props.modelId] as const,
  async ([show, modelId]) => {
    if (!show || !modelId) return;
    activeTab.value = 'basic';
    await loadDetail(modelId);
  },
  { immediate: true }
);

async function loadDetail(modelId: CommonType.IdType) {
  loading.value = true;
  const { data, error } = await fetchGetLlmModelDetail(modelId);
  if (!error) {
    detail.value = data || null;
  }
  loading.value = false;
}

function close() {
  visible.value = false;
}

function formatInteger(value?: number | null) {
  return Number(value || 0).toLocaleString('zh-CN');
}
</script>

<template>
  <NModal v-model:show="visible" :show-icon="false" :mask-closable="false" class="model-detail-modal">
    <div class="detail-panel">
      <button type="button" class="detail-close" aria-label="关闭" @click="close">
        <SvgIcon icon="material-symbols:close-rounded" class="text-24px" />
      </button>

      <NSpin :show="loading">
        <header class="detail-header">
          <div class="detail-icon">
            <SvgIcon icon="material-symbols:smart-toy-outline-rounded" class="text-26px" />
          </div>
          <div>
            <h2 class="detail-title">{{ model?.displayName || model?.modelCode || '-' }}</h2>
          </div>
        </header>

        <section class="metric-grid">
          <div class="metric-card">
            <span>输入token总计</span>
            <strong>{{ formatInteger(usageStats.promptTokens) }}</strong>
          </div>
          <div class="metric-card">
            <span>输出token总计</span>
            <strong>{{ formatInteger(usageStats.completionTokens) }}</strong>
          </div>
          <div class="metric-card">
            <span>总token</span>
            <strong>{{ formatInteger(usageStats.totalTokens) }}</strong>
          </div>
          <div class="metric-card metric-card--success">
            <span>调用次数</span>
            <strong>{{ formatInteger(usageStats.requestCount) }}</strong>
          </div>
        </section>

        <nav class="detail-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="detail-tab"
            :class="{ 'detail-tab--active': activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </nav>

        <section class="detail-body">
          <template v-if="activeTab === 'basic'">
            <div class="basic-grid">
              <div>
                <span>模型名称</span>
                <strong>{{ model?.displayName || '-' }}</strong>
              </div>
              <div>
                <span>模型编码</span>
                <strong>{{ model?.modelCode || '-' }}</strong>
              </div>
              <div>
                <span>厂商</span>
                <strong>{{ model?.provider || '-' }}</strong>
              </div>
              <div>
                <span>供应商</span>
                <strong>{{ model?.supplier || '-' }}</strong>
              </div>
              <div>
                <span>协议</span>
                <strong>{{ model?.protocol || '-' }}</strong>
              </div>
              <div>
                <span>模型类型</span>
                <strong>{{ model?.modelType || '-' }}</strong>
              </div>
              <div>
                <span>状态</span>
                <strong>{{ model?.status === '0' ? '正常' : '停用' }}</strong>
              </div>
              <div class="basic-grid__wide">
                <span>API Base</span>
                <strong>{{ model?.apiBase || '-' }}</strong>
              </div>
              <div class="basic-grid__wide">
                <span>备注</span>
                <strong>{{ model?.remark || '-' }}</strong>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="usage-grid">
              <div>
                <span>总 token</span>
                <strong>{{ formatInteger(usageStats.totalTokens) }}</strong>
              </div>
              <div>
                <span>输入 token</span>
                <strong>{{ formatInteger(usageStats.promptTokens) }}</strong>
              </div>
              <div>
                <span>输出 token</span>
                <strong>{{ formatInteger(usageStats.completionTokens) }}</strong>
              </div>
              <div>
                <span>调用次数</span>
                <strong>{{ formatInteger(usageStats.requestCount) }}</strong>
              </div>
            </div>
          </template>
        </section>
      </NSpin>
    </div>
  </NModal>
</template>

<style scoped>
.detail-panel {
  position: relative;
  width: min(980px, calc(100vw - 48px));
  min-height: 620px;
  overflow: hidden;
  border: 1px solid rgb(var(--primary-color) / 12%);
  border-radius: 2px;
  background: #fff;
  color: #111827;
}

.detail-close {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 2;
  border: 0;
  background: transparent;
  color: #4c4658;
  cursor: pointer;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 28px 20px;
}

.detail-icon {
  display: flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgb(var(--primary-color));
  color: #fff;
}

.detail-title {
  margin: 0;
  font-size: 22px;
  font-weight: 650;
  line-height: 1.2;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 0 28px 22px;
}

.metric-card {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid rgb(var(--primary-color) / 14%);
  border-radius: 8px;
  background: rgb(var(--primary-color) / 3%);
}

.metric-card span {
  display: block;
  color: #4f4b5f;
  font-size: 13px;
  font-weight: 600;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: rgb(var(--primary-color));
  font-size: 22px;
  font-weight: 750;
  line-height: 1;
}

.metric-card--success strong {
  color: #35a46f;
}

.detail-tabs {
  display: flex;
  gap: 12px;
  border-top: 1px solid rgb(var(--primary-color) / 20%);
  border-bottom: 1px solid rgb(var(--primary-color) / 20%);
  padding: 0 28px;
}

.detail-tab {
  position: relative;
  min-width: 92px;
  border: 0;
  background: transparent;
  color: #464253;
  font-size: 16px;
  font-weight: 650;
  line-height: 52px;
  cursor: pointer;
}

.detail-tab--active {
  color: rgb(var(--primary-color));
}

.detail-tab--active::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  background: rgb(var(--primary-color));
  content: '';
}

.detail-body {
  padding: 24px 28px 44px;
}

.basic-grid,
.usage-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.basic-grid > div,
.usage-grid > div {
  min-height: 64px;
  border: 1px solid rgb(var(--primary-color) / 14%);
  border-radius: 8px;
  background: rgb(var(--primary-color) / 3%);
  padding: 12px 14px;
}

.basic-grid > div span,
.usage-grid > div span {
  display: block;
  color: #6f6b7c;
  font-size: 13px;
  font-weight: 600;
}

.basic-grid > div strong,
.usage-grid > div strong {
  display: block;
  margin-top: 6px;
  color: #1d2333;
  font-size: 15px;
  font-weight: 650;
}

.basic-grid__wide {
  grid-column: span 2;
}

@media (max-width: 900px) {
  .detail-panel {
    width: calc(100vw - 24px);
    min-height: 0;
  }

  .metric-grid,
  .basic-grid,
  .usage-grid {
    grid-template-columns: 1fr;
  }

  .basic-grid__wide {
    grid-column: auto;
  }
}
</style>
