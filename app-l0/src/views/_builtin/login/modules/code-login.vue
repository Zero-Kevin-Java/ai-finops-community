<script setup lang="ts">
import { computed, reactive } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { useCaptcha } from '@/hooks/business/captcha';
import { $t } from '@/locales';
import LoginModeSwitch from './login-mode-switch.vue';

defineOptions({
  name: 'CodeLogin'
});

const { formRef, validate } = useNaiveForm();
const { label, isCounting, loading, getCaptcha } = useCaptcha();
const authStore = useAuthStore();

interface FormModel {
  phone: string;
  code: string;
}

const model: FormModel = reactive({
  phone: '',
  code: ''
});

const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  const { formRules } = useFormRules();

  return {
    phone: formRules.phone,
    code: formRules.code
  };
});

async function handleSubmit() {
  await validate();
  await authStore.login({
    phonenumber: model.phone,
    smsCode: model.code,
    grantType: 'sms'
  });
}
</script>

<template>
  <div>
    <div class="mb-5px text-32px text-black font-600 sm:text-30px dark:text-white">
      {{ $t('page.login.codeLogin.title') }}
    </div>
    <div class="pb-18px text-16px text-#858585">请输入您的手机号，我们将发送验证码到您的手机</div>
    <LoginModeSwitch active="code-login" />
    <NForm
      ref="formRef"
      :model="model"
      :rules="rules"
      size="large"
      :show-label="false"
      @keyup.enter="() => !authStore.loginLoading && handleSubmit()"
    >
      <NFormItem path="phone">
        <NInput v-model:value="model.phone" :placeholder="$t('page.login.common.phonePlaceholder')" />
      </NFormItem>
      <NFormItem path="code">
        <div class="w-full flex-y-center gap-16px">
          <NInput v-model:value="model.code" :placeholder="$t('page.login.common.codePlaceholder')" />
          <NButton
            size="large"
            class="captcha-button"
            :disabled="loading || isCounting"
            :loading="loading"
            @click="getCaptcha(model.phone)"
          >
            {{ label }}
          </NButton>
        </div>
      </NFormItem>
      <NSpace vertical :size="20" class="w-full">
        <NButton type="primary" size="large" block :loading="authStore.loginLoading" @click="handleSubmit">
          {{ $t('page.login.common.codeLogin') }}
        </NButton>
      </NSpace>
    </NForm>
  </div>
</template>

<style scoped>
:deep(.n-base-selection),
:deep(.n-input) {
  --n-height: 42px !important;
  --n-font-size: 16px !important;
  --n-border-radius: 8px !important;
}

:deep(.n-base-selection-label) {
  padding: 0 6px !important;
}

:deep(.n-button) {
  --n-height: 42px !important;
  --n-font-size: 18px !important;
  --n-border-radius: 8px !important;
}

.captcha-button {
  min-width: 136px;
  flex-shrink: 0;
}
</style>
