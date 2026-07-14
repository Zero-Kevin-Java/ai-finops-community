<script setup lang="ts">
import { computed, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useDict } from '@/hooks/business/dict';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ApiKeySearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();
const { options: keyStatusOptions } = useDict('llm_key_status');
const statusOptions = computed(() => keyStatusOptions.value.filter(option => option.value !== '2'));

const model = defineModel<Api.Llm.ApiKeySearchParams>('model', { required: true });

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
    <NCollapse :default-expanded-names="['api-key-search']">
      <NCollapseItem :title="$t('common.search')" name="api-key-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.apiKey.clientId')" path="clientId" class="pr-24px">
              <LlmAppClientSelect
                v-model:value="model.clientId"
                clearable
                :placeholder="$t('page.llm.apiKey.form.clientId.required')"
              />
            </NFormItemGi>
            <NFormItemGi
              span="24 s:12 m:6"
              :label="$t('page.llm.apiKey.ownerUserId')"
              path="ownerUserId"
              class="pr-24px"
            >
              <UserSelect
                v-model:value="model.ownerUserId"
                clearable
                filterable
                :placeholder="$t('page.llm.apiKey.form.ownerUserId.required')"
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.apiKey.keyName')" path="keyName" class="pr-24px">
              <NInput v-model:value="model.keyName" :placeholder="$t('page.llm.apiKey.form.keyName.required')" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.apiKey.keyPrefix')" path="keyPrefix" class="pr-24px">
              <NInput v-model:value="model.keyPrefix" :placeholder="$t('page.llm.apiKey.keyPrefix')" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:6" :label="$t('page.llm.apiKey.status')" path="status" class="pr-24px">
              <NSelect
                v-model:value="model.status"
                :placeholder="$t('page.llm.apiKey.form.status.required')"
                :options="statusOptions"
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
