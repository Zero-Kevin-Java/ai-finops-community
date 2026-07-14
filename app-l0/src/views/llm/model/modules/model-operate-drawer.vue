<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateLlmModel, fetchUpdateLlmModel } from '@/service/api/llm/model';
import { fetchGetLlmProviderOptions } from '@/service/api/llm/provider';
import {
  fetchDeleteSimpleRoute,
  fetchGetSimpleRoute,
  fetchGetSimpleRouteTargets,
  fetchSaveSimpleRoute
} from '@/service/api/simpleRoute';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';
import { renderProviderSelectLabel, renderProviderSelectTag } from '../../provider/modules/provider-logo';
import {
  buildModelDraft,
  buildModelDrafts,
  buildSimpleRoutePayloads,
  filterSimpleTaskTargetOptions,
  normalizeModelCodeSelection,
  type ModelDraft,
  type SimpleTaskTargetOption
} from './model-selection';

defineOptions({
  name: 'ModelOperateDrawer'
});

interface Props {
  /** the type of operation */
  operateType: NaiveUI.TableOperateType;
  /** the edit row data */
  rowData?: Api.Llm.Model | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { formRef, validate, restoreValidation } = useNaiveForm();
const { createRequiredRule } = useFormRules();

const title = computed(() => {
  const titles: Record<NaiveUI.TableOperateType, string> = {
    add: $t('page.llm.model.addModel'),
    edit: $t('page.llm.model.editModel')
  };
  return titles[props.operateType];
});

type Model = Api.Llm.ModelOperateParams;

const model = ref<Model>(createDefaultModel());
const providerOptions = ref<Api.Llm.ProviderOption[]>([]);
const selectedModelCodes = ref<string[]>([]);
const selectedModelDrafts = ref<ModelDraft[]>([]);
const autoMatchedProvider = ref('');
const simpleTaskTargetModel = ref<string | null>(null);
const simpleTaskTargetOptions = ref<SimpleTaskTargetOption[]>([]);
const simpleTaskRouteId = ref<CommonType.IdType | null>(null);

const apiBasePlaceholder = computed(() => {
  if (model.value.protocol === 'anthropic') {
    return 'https://api.anthropic.com';
  }
  return 'https://api.openai.com/v1';
});

const simpleRouteOriginalModels = computed(() => {
  if (props.operateType === 'add') {
    return selectedModelCodes.value;
  }

  return model.value.modelCode || '';
});

const filteredSimpleTaskTargetOptions = computed(() =>
  filterSimpleTaskTargetOptions(simpleTaskTargetOptions.value, simpleRouteOriginalModels.value)
);

function createDefaultModel(): Model {
  return {
    modelCode: '',
    displayName: '',
    provider: '',
    supplier: '',
    litellmModel: '',
    protocol: 'openai',
    apiKey: '',
    apiBase: '',
    modelType: 'chat',
    status: '0',
    remark: ''
  };
}

type RuleKey = Extract<keyof Model, 'modelCode' | 'displayName' | 'modelType' | 'status'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  modelCode: createRequiredRule($t('page.llm.model.form.modelCode.required')),
  displayName: createRequiredRule($t('page.llm.model.form.displayName.required')),
  modelType: createRequiredRule($t('page.llm.model.form.modelType.required')),
  status: createRequiredRule($t('page.llm.model.form.status.required'))
};

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();
  selectedModelCodes.value = [];
  selectedModelDrafts.value = [];
  autoMatchedProvider.value = '';
  simpleTaskTargetModel.value = null;
  simpleTaskRouteId.value = null;

  if (props.operateType === 'edit' && props.rowData) {
    Object.assign(model.value, jsonClone(props.rowData));
    selectedModelCodes.value = normalizeModelCodeSelection(model.value.modelCode);
  }
}

function closeDrawer() {
  visible.value = false;
}

async function handleSubmit() {
  await validate();

  const { modelId, modelCode, displayName, provider, supplier, protocol, apiKey, apiBase, modelType, status, remark } =
    model.value;
  const normalizedLitellmModel = '';
  const normalizedProvider = provider || autoMatchedProvider.value || '';

  if (props.operateType === 'add') {
    const modelCodes = normalizeModelCodeSelection(
      selectedModelCodes.value.length ? selectedModelCodes.value : modelCode
    );
    const drafts = selectedModelDrafts.value.length
      ? selectedModelDrafts.value
      : buildModelDrafts(modelCodes, getMatchedProviderName);

    if (drafts.some(draft => !draft.displayName.trim())) {
      window.$message?.error($t('page.llm.model.form.displayName.invalid'));
      return;
    }

    for (const draft of drafts) {
      const { error } = await fetchCreateLlmModel({
        modelCode: draft.modelCode,
        displayName: draft.displayName.trim(),
        provider: draft.provider || normalizedProvider,
        supplier,
        litellmModel: normalizedLitellmModel,
        protocol,
        apiKey,
        apiBase,
        modelType: draft.modelType || modelType,
        status,
        remark
      });
      if (error) return;
    }

    const simpleRoutePayloads = buildSimpleRoutePayloads(
      drafts.map(draft => draft.modelCode),
      simpleTaskTargetModel.value
    );
    for (const payload of simpleRoutePayloads) {
      const { error } = await fetchSaveSimpleRoute(payload);
      if (error) return;
    }

    window.$message?.success($t('common.addSuccess'));
  }

  if (props.operateType === 'edit') {
    const { error } = await fetchUpdateLlmModel({
      modelId,
      modelCode,
      displayName,
      provider: normalizedProvider,
      supplier,
      litellmModel: normalizedLitellmModel,
      protocol,
      apiKey,
      apiBase,
      modelType,
      status,
      remark
    });
    if (error) return;

    const [simpleRoutePayload] = buildSimpleRoutePayloads([modelCode || ''], simpleTaskTargetModel.value);
    if (simpleRoutePayload) {
      const { error: routeError } = await fetchSaveSimpleRoute(simpleRoutePayload);
      if (routeError) return;
    } else if (simpleTaskRouteId.value) {
      const { error: routeError } = await fetchDeleteSimpleRoute([simpleTaskRouteId.value]);
      if (routeError) return;
    }

    window.$message?.success($t('common.updateSuccess'));
  }

  closeDrawer();
  emit('submitted');
}

watch(visible, () => {
  if (visible.value) {
    handleUpdateModelWhenEdit();
    loadProviderOptions();
    loadSimpleTaskTargetOptions();
    loadSimpleRouteWhenEdit();
    restoreValidation();
  }
});

watch(
  () => model.value.modelCode,
  value => {
    if (props.operateType === 'edit') {
      matchProviderByModelCode(value);
    }
  }
);

// 协议切换时自动填充 apiBase 默认值
watch(
  () => model.value.protocol,
  val => {
    if (!model.value.apiBase) {
      if (val === 'anthropic') {
        model.value.apiBase = 'https://api.anthropic.com';
      } else if (val === 'openai') {
        model.value.apiBase = 'https://api.openai.com/v1';
      }
    }
  }
);

async function loadProviderOptions() {
  const { data } = await fetchGetLlmProviderOptions();
  providerOptions.value = data || [];
  matchProviderByModelCode(model.value.modelCode);
  refreshSelectedModelDrafts();
}

async function loadSimpleTaskTargetOptions() {
  const { data } = await fetchGetSimpleRouteTargets();
  simpleTaskTargetOptions.value = (data || []).map(item => ({
    label: item.displayName ? `${item.displayName}（${item.modelCode}）` : item.modelCode,
    value: item.modelCode
  }));
}

async function loadSimpleRouteWhenEdit() {
  simpleTaskTargetModel.value = null;
  simpleTaskRouteId.value = null;

  if (props.operateType !== 'edit' || !model.value.modelCode) return;

  const { data } = await fetchGetSimpleRoute(model.value.modelCode);
  simpleTaskTargetModel.value = data?.targetModel || null;
  simpleTaskRouteId.value = data?.id || null;
  clearInvalidSimpleTaskTarget();
}

function matchProviderByModelCode(modelCode?: string | null) {
  const matched = findMatchedProvider(modelCode);
  if (!matched?.providerName) return;

  if (!model.value.provider || model.value.provider === autoMatchedProvider.value) {
    model.value.provider = matched.providerName;
    autoMatchedProvider.value = matched.providerName;
  }
}

function getMatchedProviderName(modelCode: string) {
  return findMatchedProvider(modelCode)?.providerName || '';
}

function findMatchedProvider(modelCode?: string | null) {
  const normalizedModelCode = modelCode?.trim().toLowerCase();
  if (!normalizedModelCode) return null;

  return providerOptions.value
    .flatMap(provider =>
      parseModelPrefixes(provider.modelPrefixes).map(prefix => ({
        provider,
        prefix
      }))
    )
    .filter(item => normalizedModelCode.startsWith(item.prefix.toLowerCase()))
    .sort((a, b) => b.prefix.length - a.prefix.length)[0]?.provider;
}

function parseModelPrefixes(value?: string | null) {
  return (value || '')
    .split(/[,，;；、\n\r\t]+/)
    .map(item => item.trim())
    .filter(Boolean);
}

function applyModelDraft(modelCode: string) {
  const draft = buildModelDraft(modelCode, getMatchedProviderName);

  model.value.modelCode = draft.modelCode;
  model.value.displayName = draft.displayName;
  model.value.provider = draft.provider;
  model.value.modelType = draft.modelType || 'chat';
  autoMatchedProvider.value = draft.provider;
}

function syncFormModelFromFirstDraft() {
  const firstDraft = selectedModelDrafts.value[0];

  if (!firstDraft) {
    model.value.modelCode = '';
    model.value.displayName = '';
    model.value.provider = '';
    autoMatchedProvider.value = '';
    return;
  }

  model.value.modelCode = firstDraft.modelCode;
  model.value.displayName = firstDraft.displayName;
  model.value.provider = firstDraft.provider;
  model.value.modelType = firstDraft.modelType || 'chat';
  autoMatchedProvider.value = firstDraft.provider;
}

function refreshSelectedModelDrafts() {
  if (props.operateType !== 'add' || !selectedModelCodes.value.length) return;

  selectedModelDrafts.value = buildModelDrafts(
    selectedModelCodes.value,
    getMatchedProviderName,
    selectedModelDrafts.value
  );
  syncFormModelFromFirstDraft();
}

function handleModelNamesUpdate(value: string[] | null) {
  selectedModelCodes.value = normalizeModelCodeSelection(value);
  selectedModelDrafts.value = buildModelDrafts(
    selectedModelCodes.value,
    getMatchedProviderName,
    selectedModelDrafts.value
  );

  syncFormModelFromFirstDraft();
  clearInvalidSimpleTaskTarget();
}

function handleModelNameUpdate(value: string | null) {
  const selectedModelCode = normalizeModelCodeSelection(value)[0];
  if (!selectedModelCode) {
    model.value.modelCode = '';
    model.value.displayName = '';
    model.value.provider = '';
    autoMatchedProvider.value = '';
    return;
  }

  applyModelDraft(selectedModelCode);
  loadSimpleRouteWhenEdit();
  clearInvalidSimpleTaskTarget();
}

function clearInvalidSimpleTaskTarget() {
  if (!simpleTaskTargetModel.value) return;

  const selectedOriginalModels = normalizeModelCodeSelection(simpleRouteOriginalModels.value);
  if (selectedOriginalModels.includes(simpleTaskTargetModel.value)) {
    simpleTaskTargetModel.value = null;
  }
}
</script>

<template>
  <NDrawer v-model:show="visible" :title="title" display-directive="show" :width="800" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <NFormItem :label="$t('page.llm.model.modelCode')" path="modelCode">
          <NSelect
            v-if="operateType === 'add'"
            v-model:value="selectedModelCodes"
            multiple
            tag
            filterable
            clearable
            max-tag-count="responsive"
            :placeholder="$t('page.llm.model.form.modelCode.required')"
            @update:value="handleModelNamesUpdate"
          />
          <NSelect
            v-else
            v-model:value="model.modelCode"
            tag
            filterable
            clearable
            :placeholder="$t('page.llm.model.form.modelCode.required')"
            @update:value="handleModelNameUpdate"
          />
        </NFormItem>
        <NFormItem v-if="operateType === 'add'" :label="$t('page.llm.model.displayName')" path="displayName">
          <NSpace vertical class="w-full">
            <NInputGroup v-for="draft in selectedModelDrafts" :key="draft.modelCode">
              <NInputGroupLabel class="min-w-200px">{{ draft.modelCode }}</NInputGroupLabel>
              <NInput v-model:value="draft.displayName" :placeholder="$t('page.llm.model.form.displayName.required')" />
            </NInputGroup>
            <NInput
              v-if="!selectedModelDrafts.length"
              v-model:value="model.displayName"
              :placeholder="$t('page.llm.model.form.displayName.required')"
              disabled
            />
          </NSpace>
        </NFormItem>
        <NFormItem v-else :label="$t('page.llm.model.displayName')" path="displayName">
          <NInput v-model:value="model.displayName" :placeholder="$t('page.llm.model.form.displayName.required')" />
        </NFormItem>
        <NFormItem v-if="operateType === 'edit'" :label="$t('page.llm.model.provider')" path="provider">
          <NSelect
            v-model:value="model.provider"
            tag
            filterable
            clearable
            :options="providerOptions"
            label-field="label"
            value-field="providerName"
            :render-label="renderProviderSelectLabel"
            :render-tag="renderProviderSelectTag"
            :placeholder="$t('page.llm.model.form.provider.required')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.supplier')" path="supplier">
          <NInput v-model:value="model.supplier" :placeholder="$t('page.llm.model.form.supplier.required')" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.protocol')" path="protocol">
          <NRadioGroup v-model:value="model.protocol">
            <NRadio value="openai">OpenAI 兼容</NRadio>
            <NRadio value="anthropic">Anthropic</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.apiKey')" path="apiKey">
          <NInput
            v-model:value="model.apiKey"
            type="password"
            show-password-on="click"
            :placeholder="operateType === 'edit' ? '留空则不修改' : 'sk-xxxx'"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.apiBase')" path="apiBase">
          <NInput v-model:value="model.apiBase" :placeholder="apiBasePlaceholder" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.litellmModel')" path="litellmModel" class="hidden">
          <NInput v-model:value="model.litellmModel" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.modelType')" path="modelType">
          <DictRadio v-model:value="model.modelType" dict-code="llm_model_type" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.simpleTaskTarget')">
          <NSelect
            v-model:value="simpleTaskTargetModel"
            filterable
            clearable
            :options="filteredSimpleTaskTargetOptions"
            :placeholder="$t('page.llm.model.simpleTaskTarget')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.status')" path="status">
          <DictRadio v-model:value="model.status" dict-code="sys_normal_disable" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.model.remark')" path="remark">
          <NInput
            v-model:value="model.remark"
            :rows="3"
            type="textarea"
            :placeholder="$t('page.llm.model.form.remark.required')"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace :size="16">
          <NButton @click="closeDrawer">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" @click="handleSubmit">{{ $t('common.save') }}</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
