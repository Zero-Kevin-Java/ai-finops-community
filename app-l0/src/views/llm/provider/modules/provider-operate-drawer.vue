<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateLlmProvider, fetchUpdateLlmProvider } from '@/service/api/llm/provider';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';
import { renderProviderLogo } from './provider-logo';

defineOptions({
  name: 'ProviderOperateDrawer'
});

interface Props {
  operateType: NaiveUI.TableOperateType;
  rowData?: Api.Llm.Provider | null;
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
    add: $t('page.llm.provider.addProvider'),
    edit: $t('page.llm.provider.editProvider')
  };
  return titles[props.operateType];
});

type Model = Api.Llm.ProviderOperateParams;

const model = ref<Model>(createDefaultModel());

const modelPrefixList = computed<string[]>({
  get() {
    return parseModelPrefixes(model.value.modelPrefixes);
  },
  set(value) {
    model.value.modelPrefixes = serializeModelPrefixes(value);
  }
});

const ProviderLogoPreview = () =>
  renderProviderLogo(
    {
      providerName: model.value.providerName || undefined,
      logoSlug: model.value.logoSlug || undefined
    },
    32
  );

function createDefaultModel(): Model {
  return {
    providerName: '',
    logoSlug: '',
    modelPrefixes: '',
    sortOrder: 0,
    status: '0',
    remark: ''
  };
}

type RuleKey = Extract<keyof Model, 'providerId' | 'providerName' | 'status'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  providerId: createRequiredRule($t('page.llm.provider.form.providerId.required')),
  providerName: createRequiredRule($t('page.llm.provider.form.providerName.required')),
  status: createRequiredRule($t('page.llm.provider.form.status.required'))
};

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();

  if (props.operateType === 'edit' && props.rowData) {
    Object.assign(model.value, jsonClone(props.rowData));
  }
}

function closeDrawer() {
  visible.value = false;
}

async function handleSubmit() {
  await validate();

  const payload: Api.Llm.ProviderOperateParams = {
    providerId: model.value.providerId,
    providerName: model.value.providerName,
    logoSlug: model.value.logoSlug,
    modelPrefixes: serializeModelPrefixes(modelPrefixList.value),
    sortOrder: model.value.sortOrder,
    status: model.value.status,
    remark: model.value.remark
  };

  if (props.operateType === 'add') {
    const { error } = await fetchCreateLlmProvider(payload);
    if (error) return;
    window.$message?.success($t('common.addSuccess'));
  }

  if (props.operateType === 'edit') {
    const { error } = await fetchUpdateLlmProvider(payload);
    if (error) return;
    window.$message?.success($t('common.updateSuccess'));
  }

  closeDrawer();
  emit('submitted');
}

watch(visible, () => {
  if (visible.value) {
    handleUpdateModelWhenEdit();
    restoreValidation();
  }
});

function parseModelPrefixes(value?: string | null) {
  return (value || '')
    .split(/[,，;；、\n\r\t]+/)
    .map(item => item.trim())
    .filter(Boolean);
}

function serializeModelPrefixes(value?: string[] | null) {
  return Array.from(new Set((value || []).map(item => item.trim()).filter(Boolean))).join(',');
}
</script>

<template>
  <NDrawer v-model:show="visible" :title="title" display-directive="show" :width="760" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <NFormItem :label="$t('page.llm.provider.providerName')" path="providerName">
          <NInput
            v-model:value="model.providerName"
            :placeholder="$t('page.llm.provider.form.providerName.required')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.provider.logoSlug')" path="logoSlug">
          <div class="w-full flex-y-center gap-12px">
            <NInput v-model:value="model.logoSlug" placeholder="DeepSeek.Color" />
            <ProviderLogoPreview />
          </div>
        </NFormItem>
        <NFormItem :label="$t('page.llm.provider.modelPrefixes')" path="modelPrefixes">
          <NDynamicTags v-model:value="modelPrefixList" class="w-full" placeholder="deepseek" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.provider.sortOrder')" path="sortOrder">
          <NInputNumber v-model:value="model.sortOrder" class="w-full" :precision="0" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.provider.status')" path="status">
          <DictRadio v-model:value="model.status" dict-code="sys_normal_disable" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.provider.remark')" path="remark">
          <NInput v-model:value="model.remark" :rows="3" type="textarea" />
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
