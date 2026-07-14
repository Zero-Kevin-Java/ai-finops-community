<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ModelSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Llm.ModelSearchParams>('model', { required: true });

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
    <NCollapse :default-expanded-names="['model-search']">
      <NCollapseItem :title="$t('common.search')" name="model-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.model.modelCode')" path="modelCode" class="pr-24px">
              <NInput v-model:value="model.modelCode" :placeholder="$t('page.llm.model.form.modelCode.required')" />
            </NFormItemGi>
            <NFormItemGi
              span="24 s:12 m:6"
              :label="$t('page.llm.model.displayName')"
              path="displayName"
              class="pr-24px"
            >
              <NInput v-model:value="model.displayName" :placeholder="$t('page.llm.model.form.displayName.required')" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.model.provider')" path="provider" class="pr-24px">
              <NInput v-model:value="model.provider" :placeholder="$t('page.llm.model.form.provider.required')" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.model.supplier')" path="supplier" class="pr-24px">
              <NInput v-model:value="model.supplier" :placeholder="$t('page.llm.model.form.supplier.required')" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.model.modelType')" path="modelType" class="pr-24px">
              <DictSelect
                v-model:value="model.modelType"
                :placeholder="$t('page.llm.model.form.modelType.required')"
                dict-code="llm_model_type"
                clearable
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.model.status')" path="status" class="pr-24px">
              <DictSelect
                v-model:value="model.status"
                :placeholder="$t('page.llm.model.form.status.required')"
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
