<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateLlmApiKey, fetchUpdateLlmApiKey } from '@/service/api/llm/api-key';
import { fetchGetLlmModelOptions } from '@/service/api/llm/model';
import { useDict } from '@/hooks/business/dict';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ApiKeyOperateDrawer'
});

interface Props {
  /** the type of operation */
  operateType: NaiveUI.TableOperateType;
  /** the edit row data */
  rowData?: Api.Llm.ApiKey | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
  (e: 'generated', plainKey: string): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { formRef, validate, restoreValidation } = useNaiveForm();
const { createRequiredRule } = useFormRules();
const { options: keyStatusOptions } = useDict('llm_key_status');
const statusOptions = computed(() => keyStatusOptions.value.filter(option => option.value !== '2'));

const title = computed(() => {
  const titles: Record<NaiveUI.TableOperateType, string> = {
    add: $t('page.llm.apiKey.addApiKey'),
    edit: $t('page.llm.apiKey.editApiKey')
  };
  return titles[props.operateType];
});

type Model = Api.Llm.ApiKeyOperateParams;

const model = ref<Model>(createDefaultModel());
const modelOptions = ref<{ label: string; value: string }[]>([]);

function createDefaultModel(): Model {
  return {
    clientId: null,
    ownerUserId: null,
    keyName: '',
    keyScope: [],
    expireTime: null,
    status: '0',
    remark: ''
  };
}

type RuleKey = Extract<keyof Model, 'clientId' | 'ownerUserId' | 'keyName' | 'keyScope' | 'status'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  clientId: createRequiredRule($t('page.llm.apiKey.form.clientId.required')),
  ownerUserId: createRequiredRule($t('page.llm.apiKey.form.ownerUserId.required')),
  keyName: createRequiredRule($t('page.llm.apiKey.form.keyName.required')),
  keyScope: { ...createRequiredRule($t('page.llm.apiKey.form.keyScope.required')), type: 'array' },
  status: createRequiredRule($t('page.llm.apiKey.form.status.required'))
};

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();

  if (props.operateType === 'edit' && props.rowData) {
    Object.assign(model.value, {
      ...jsonClone(props.rowData),
      keyScope: parseModelScope(props.rowData.keyScope)
    });
  }
}

function parseModelScope(value?: string | null): string[] {
  if (!value) return [];
  const trimmed = value.trim();
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed);
      return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
    } catch {
      return [];
    }
  }
  return trimmed
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
}

async function loadModelOptions() {
  const { data } = await fetchGetLlmModelOptions();
  modelOptions.value = [{ label: '全部模型', value: '*' }, ...(data || [])];
}

function closeDrawer() {
  visible.value = false;
}

async function handleSubmit() {
  await validate();

  const { keyId, clientId, ownerUserId, keyName, keyScope, expireTime, status, remark } = model.value;

  if (props.operateType === 'add') {
    const { data, error } = await fetchCreateLlmApiKey({
      clientId,
      ownerUserId,
      keyName,
      keyScope,
      expireTime,
      status,
      remark
    });
    if (error) return;
    window.$message?.success($t('common.addSuccess'));
    if (data) {
      emit('generated', data);
    }
  }

  if (props.operateType === 'edit') {
    const { error } = await fetchUpdateLlmApiKey({
      keyId,
      clientId,
      ownerUserId,
      keyName,
      keyScope,
      expireTime,
      status,
      remark
    });
    if (error) return;
    window.$message?.success($t('common.updateSuccess'));
  }

  closeDrawer();
  emit('submitted');
}

watch(visible, () => {
  if (visible.value) {
    handleUpdateModelWhenEdit();
    loadModelOptions();
    restoreValidation();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" :title="title" display-directive="show" :width="720" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <NFormItem :label="$t('page.llm.apiKey.clientId')" path="clientId">
          <LlmAppClientSelect
            v-model:value="model.clientId"
            clearable
            :placeholder="$t('page.llm.apiKey.form.clientId.required')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.apiKey.ownerUserId')" path="ownerUserId">
          <UserSelect
            v-model:value="model.ownerUserId"
            clearable
            filterable
            :placeholder="$t('page.llm.apiKey.form.ownerUserId.required')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.apiKey.keyName')" path="keyName">
          <NInput v-model:value="model.keyName" :placeholder="$t('page.llm.apiKey.form.keyName.required')" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.apiKey.keyScope')" path="keyScope">
          <NSelect
            v-model:value="model.keyScope"
            :options="modelOptions"
            multiple
            filterable
            clearable
            :placeholder="$t('page.llm.apiKey.form.keyScope.required')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.apiKey.expireTime')" path="expireTime">
          <NDatePicker
            v-model:formatted-value="model.expireTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            clearable
            class="w-full"
            :placeholder="$t('page.llm.apiKey.form.expireTime.never')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.apiKey.status')" path="status">
          <NRadioGroup v-model:value="model.status">
            <NSpace>
              <NRadio v-for="option in statusOptions" :key="option.value" :value="option.value" :label="option.label" />
            </NSpace>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('page.llm.apiKey.remark')" path="remark">
          <NInput
            v-model:value="model.remark"
            :rows="3"
            type="textarea"
            :placeholder="$t('page.llm.apiKey.form.remark.required')"
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
