<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateLlmAppClient, fetchUpdateLlmAppClient } from '@/service/api/llm/app-client';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'AppClientOperateDrawer'
});

interface Props {
  /** the type of operation */
  operateType: NaiveUI.TableOperateType;
  /** the edit row data */
  rowData?: Api.Llm.AppClient | null;
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
    add: $t('page.llm.appClient.addAppClient'),
    edit: $t('page.llm.appClient.editAppClient')
  };
  return titles[props.operateType];
});

type Model = Api.Llm.AppClientOperateParams;

const model = ref<Model>(createDefaultModel());

function createDefaultModel(): Model {
  return {
    projectId: null,
    appCode: '',
    appName: '',
    appType: 'server',
    status: '0',
    remark: ''
  };
}

type RuleKey = Extract<keyof Model, 'projectId' | 'appCode' | 'appName' | 'appType' | 'status'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  projectId: createRequiredRule($t('page.llm.appClient.form.projectId.required')),
  appCode: createRequiredRule($t('page.llm.appClient.form.appCode.required')),
  appName: createRequiredRule($t('page.llm.appClient.form.appName.required')),
  appType: createRequiredRule($t('page.llm.appClient.form.appType.required')),
  status: createRequiredRule($t('page.llm.appClient.form.status.required'))
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

  const { clientId, projectId, appCode, appName, appType, status, remark } = model.value;

  if (props.operateType === 'add') {
    const { error } = await fetchCreateLlmAppClient({
      projectId,
      appCode,
      appName,
      appType,
      status,
      remark
    });
    if (error) return;
    window.$message?.success($t('common.addSuccess'));
  }

  if (props.operateType === 'edit') {
    const { error } = await fetchUpdateLlmAppClient({
      clientId,
      projectId,
      appCode,
      appName,
      appType,
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
    restoreValidation();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" :title="title" display-directive="show" :width="720" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <NFormItem :label="$t('page.llm.appClient.projectId')" path="projectId">
          <LlmProjectSelect
            v-model:value="model.projectId"
            clearable
            :placeholder="$t('page.llm.appClient.form.projectId.required')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.appClient.appCode')" path="appCode">
          <NInput v-model:value="model.appCode" :placeholder="$t('page.llm.appClient.form.appCode.required')" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.appClient.appName')" path="appName">
          <NInput v-model:value="model.appName" :placeholder="$t('page.llm.appClient.form.appName.required')" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.appClient.appType')" path="appType">
          <DictRadio v-model:value="model.appType" dict-code="llm_app_type" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.appClient.status')" path="status">
          <DictRadio v-model:value="model.status" dict-code="sys_normal_disable" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.appClient.remark')" path="remark">
          <NInput
            v-model:value="model.remark"
            :rows="3"
            type="textarea"
            :placeholder="$t('page.llm.appClient.form.remark.required')"
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
