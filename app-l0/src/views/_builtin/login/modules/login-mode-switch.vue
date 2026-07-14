<script setup lang="ts">
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';

defineOptions({
  name: 'LoginModeSwitch'
});

interface Props {
  active: Extract<UnionKey.LoginModule, 'pwd-login' | 'code-login'>;
}

defineProps<Props>();

const { toggleLoginModule } = useRouterPush();

function handleSwitch(module: Props['active']) {
  toggleLoginModule(module);
}
</script>

<template>
  <div class="mb-20px grid grid-cols-2 gap-4px rounded-8px bg-#f3f4f6 p-4px dark:bg-#25272d">
    <button
      type="button"
      class="login-mode-item"
      :class="{ 'login-mode-item_active': active === 'pwd-login' }"
      @click="handleSwitch('pwd-login')"
    >
      {{ $t('page.login.pwdLogin.title') }}
    </button>
    <button
      type="button"
      class="login-mode-item"
      :class="{ 'login-mode-item_active': active === 'code-login' }"
      @click="handleSwitch('code-login')"
    >
      {{ $t('page.login.codeLogin.title') }}
    </button>
  </div>
</template>

<style scoped>
.login-mode-item {
  height: 38px;
  border-radius: 6px;
  color: #858585;
  font-size: 16px;
  line-height: 38px;
  transition:
    background-color 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease;
}

.login-mode-item:hover {
  color: rgb(var(--primary-color));
}

.login-mode-item_active {
  background: #fff;
  color: rgb(var(--primary-color));
  box-shadow: 0 4px 14px rgb(15 23 42 / 8%);
}

.dark .login-mode-item_active {
  background: #18181c;
  box-shadow: none;
}
</style>
