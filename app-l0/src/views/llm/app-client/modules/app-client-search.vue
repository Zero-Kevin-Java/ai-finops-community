<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'AppClientSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Llm.AppClientSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

function resetModel() {
  Object.assign(model.value, defaultModel);
}

async function reset() {
  await restoreValidation();
  resetModel();
  emit('search');
}

async function search() {
  await validate();
  emit('search');
}
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NCollapse :default-expanded-names="['app-client-search']">
      <NCollapseItem :title="$t('common.search')" name="app-client-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi
              span="24 s:12 m:6"
              :label="$t('page.llm.appClient.projectId')"
              path="projectId"
              class="pr-24px"
            >
              <LlmProjectSelect
                v-model:value="model.projectId"
                clearable
                :placeholder="$t('page.llm.appClient.form.projectId.required')"
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.appClient.appCode')" path="appCode" class="pr-24px">
              <NInput v-model:value="model.appCode" :placeholder="$t('page.llm.appClient.form.appCode.required')" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.appClient.appName')" path="appName" class="pr-24px">
              <NInput v-model:value="model.appName" :placeholder="$t('page.llm.appClient.form.appName.required')" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.appClient.appType')" path="appType" class="pr-24px">
              <DictSelect
                v-model:value="model.appType"
                :placeholder="$t('page.llm.appClient.form.appType.required')"
                dict-code="llm_app_type"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.appClient.status')" path="status" class="pr-24px">
              <DictSelect
                v-model:value="model.status"
                :placeholder="$t('page.llm.appClient.form.status.required')"
                dict-code="sys_normal_disable"
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

<style scoped></style>
