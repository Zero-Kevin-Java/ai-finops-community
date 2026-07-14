<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateLlmProject, fetchUpdateLlmProject } from '@/service/api/llm/project';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ProjectOperateDrawer'
});

interface Props {
  /** the type of operation */
  operateType: NaiveUI.TableOperateType;
  /** the edit row data */
  rowData?: Api.Llm.Project | null;
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
    add: $t('page.llm.project.addProject'),
    edit: $t('page.llm.project.editProject')
  };
  return titles[props.operateType];
});

type Model = Api.Llm.ProjectOperateParams;

const model = ref<Model>(createDefaultModel());

function createDefaultModel(): Model {
  return {
    projectCode: '',
    projectName: '',
    ownerUserId: null,
    status: '0',
    remark: ''
  };
}

type RuleKey = Extract<keyof Model, 'projectCode' | 'projectName' | 'ownerUserId' | 'status'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  projectCode: createRequiredRule($t('page.llm.project.form.projectCode.required')),
  projectName: createRequiredRule($t('page.llm.project.form.projectName.required')),
  ownerUserId: createRequiredRule($t('page.llm.project.form.ownerUserId.required')),
  status: createRequiredRule($t('page.llm.project.form.status.required'))
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

  const { projectId, projectCode, projectName, ownerUserId, status, remark } = model.value;

  if (props.operateType === 'add') {
    const { error } = await fetchCreateLlmProject({
      projectCode,
      projectName,
      ownerUserId,
      status,
      remark
    });
    if (error) return;
    window.$message?.success($t('common.addSuccess'));
  }

  if (props.operateType === 'edit') {
    const { error } = await fetchUpdateLlmProject({
      projectId,
      projectCode,
      projectName,
      ownerUserId,
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
        <NFormItem :label="$t('page.llm.project.projectCode')" path="projectCode">
          <NInput v-model:value="model.projectCode" :placeholder="$t('page.llm.project.form.projectCode.required')" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.project.projectName')" path="projectName">
          <NInput v-model:value="model.projectName" :placeholder="$t('page.llm.project.form.projectName.required')" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.project.ownerUserId')" path="ownerUserId">
          <UserSelect
            v-model:value="model.ownerUserId"
            class="w-full"
            clearable
            filterable
            :placeholder="$t('page.llm.project.form.ownerUserId.required')"
          />
        </NFormItem>
        <NFormItem :label="$t('page.llm.project.status')" path="status">
          <DictRadio v-model:value="model.status" dict-code="sys_normal_disable" />
        </NFormItem>
        <NFormItem :label="$t('page.llm.project.remark')" path="remark">
          <NInput
            v-model:value="model.remark"
            :rows="3"
            type="textarea"
            :placeholder="$t('page.llm.project.form.remark.required')"
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
