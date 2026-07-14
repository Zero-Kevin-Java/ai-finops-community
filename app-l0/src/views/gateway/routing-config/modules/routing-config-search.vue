<script setup lang="ts">
import { ref, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'RoutingConfigSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const model = defineModel<Api.Gateway.RoutingConfigSearchParams>('model', {
  required: true
});

const { formRef, validate, restoreValidation } = useNaiveForm();
const effectiveStartRange = ref<[string, string] | null>(null);
const matchLogic = ref<Api.Gateway.RoutingConditionLogic | null>(null);
const fallbackMode = ref<Api.Gateway.RoutingFallbackConfig['fallbackMode'] | null>(null);

const defaultModel = jsonClone(toRaw(model.value));

function resetModel() {
  effectiveStartRange.value = null;
  matchLogic.value = null;
  fallbackMode.value = null;
  Object.assign(model.value, defaultModel);
  model.value.params = {};
}

function syncExtraParams() {
  model.value.params = {
    ...model.value.params,
    matchLogic: matchLogic.value,
    fallbackMode: fallbackMode.value,
    beginTime: effectiveStartRange.value?.[0] || null,
    endTime: effectiveStartRange.value?.[1] || null
  };
}

async function reset() {
  await restoreValidation();
  resetModel();
  emit('search');
}

async function search() {
  syncExtraParams();
  await validate();
  emit('search');
}
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NCollapse :default-expanded-names="['routing-config-search']">
      <NCollapseItem :title="$t('common.search')" name="routing-config-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi span="24 s:12 m:6" label="规则名称" path="ruleName" class="pr-24px">
              <NInput v-model:value="model.ruleName" placeholder="按规则名称搜索" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" label="命中逻辑" class="pr-24px">
              <NSelect
                v-model:value="matchLogic"
                placeholder="全部逻辑"
                :options="[
                  { label: '全部满足', value: 'ALL' },
                  { label: '任一条件', value: 'ANY' }
                ]"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" label="命中动作" path="actionType" class="pr-24px">
              <NSelect
                v-model:value="model.actionType"
                placeholder="全部动作"
                :options="[
                  { label: '执行项目分类器', value: 'CLASSIFIER' },
                  { label: '不拦截', value: 'ORIGINAL_MODEL' },
                  { label: '指定模型', value: 'TARGET_MODEL' },
                  { label: '拦截', value: 'DENY' }
                ]"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" label="执行方式" path="executionMode" class="pr-24px">
              <NSelect
                v-model:value="model.executionMode"
                placeholder="全部方式"
                :options="[
                  { label: '生效执行', value: 'ENFORCE' },
                  { label: '仅记录不执行', value: 'RECORD_ONLY' }
                ]"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" label="兜底模型" class="pr-24px">
              <NSelect
                v-model:value="fallbackMode"
                placeholder="全部兜底方式"
                :options="[
                  { label: '回到原模型', value: 'ORIGINAL_MODEL' },
                  { label: '使用兜底模型', value: 'TARGET_MODEL' },
                  { label: '拦截', value: 'DENY' }
                ]"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" label="状态" path="status" class="pr-24px">
              <NSelect
                v-model:value="model.status"
                placeholder="全部状态"
                :options="[
                  { label: '启用', value: '0' },
                  { label: '停用', value: '1' }
                ]"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" label="生效开始" class="pr-24px">
              <NDatePicker
                v-model:formatted-value="effectiveStartRange"
                class="w-full"
                type="datetimerange"
                value-format="yyyy-MM-dd HH:mm:ss"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" class="pr-24px" suffix>
              <NSpace class="w-full" justify="end">
                <NButton @click="reset">
                  <template #icon>
                    <icon-ic-round-refresh class="text-icon" />
                  </template>
                  {{ $t('common.reset') }}
                </NButton>
                <NButton type="primary" ghost @click="search">
                  <template #icon>
                    <icon-ic-round-search class="text-icon" />
                  </template>
                  {{ $t('common.search') }}
                </NButton>
              </NSpace>
            </NFormItemGi>
          </NGrid>
        </NForm>
      </NCollapseItem>
    </NCollapse>
  </NCard>
</template>
