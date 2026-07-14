<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { SelectOption } from 'naive-ui';
import { useLoading } from '@sa/hooks';
import { fetchTenantList } from '@/service/api';
import { fetchChangeTenant, fetchClearTenant } from '@/service/api/system/tenant';
import { fetchGetUserTenantList, fetchSwitchUserTenant } from '@/service/api/system/user';
import { useAppStore } from '@/store/modules/app';
import { useTabStore } from '@/store/modules/tab';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';

defineOptions({ name: 'TenantSelect' });

interface Props {
  clearable?: boolean;
}

withDefaults(defineProps<Props>(), {
  clearable: false
});

const appStore = useAppStore();
const authStore = useAuthStore();
const { userInfo } = authStore;
const { clearTabs } = useTabStore();
const { toHome } = useRouterPush();

const tenantId = defineModel<CommonType.IdType>('tenantId', { required: false, default: undefined });
const enabled = defineModel<boolean>('enabled', { required: false, default: false });

const lastSelected = ref<CommonType.IdType>();

const tenantOption = ref<SelectOption[]>([]);
const { loading, startLoading, endLoading } = useLoading();

const isSuperAdmin = computed<boolean>(() => {
  return userInfo.user?.userId === 1;
});

const showTenantSelect = computed<boolean>(() => {
  return enabled.value && tenantOption.value.length > 1;
});

/**
 * 关闭当前页面并刷新
 *
 * @param msg 提示信息
 * @param val 租户ID
 */
async function closeAndRefresh(msg: string, val: CommonType.IdType = '') {
  lastSelected.value = val;
  window.$message?.success(msg);
  clearTabs([], true);
  toHome();
  appStore.reloadPage(500);
}

async function handleChangeTenant(_tenantId: CommonType.IdType) {
  if (!_tenantId) {
    return;
  }
  if (lastSelected.value === _tenantId) {
    return;
  }
  if (isSuperAdmin.value) {
    await fetchChangeTenant(_tenantId);
  } else {
    const { data, error } = await fetchSwitchUserTenant(_tenantId);
    if (error || !data) {
      return;
    }
    const switched = await authStore.loginByToken(data);
    if (!switched) {
      return;
    }
  }
  closeAndRefresh('切换租户成功', _tenantId);
}

async function handleClearTenant() {
  if (!isSuperAdmin.value) {
    return;
  }
  await fetchClearTenant();
  closeAndRefresh('切换为默认租户');
}

async function handleFetchTenantList() {
  startLoading();
  const { data, error } = await fetchTenantList();
  if (error) return;
  enabled.value = data.tenantEnabled;
  if (data.tenantEnabled) {
    tenantOption.value = data.voList.map(tenant => {
      return {
        label: tenant.companyName,
        value: tenant.tenantId
      };
    });
  }
  endLoading();
}

async function handleFetchUserTenantList() {
  startLoading();
  const { data, error } = await fetchGetUserTenantList();
  if (error || !data) {
    endLoading();
    return;
  }
  enabled.value = data.length > 1;
  tenantOption.value = data.map(tenant => {
    if (tenant.current) {
      lastSelected.value = tenant.tenantId;
      tenantId.value = tenant.tenantId;
    }
    return {
      label: tenant.companyName || String(tenant.tenantId),
      value: tenant.tenantId
    };
  });
  endLoading();
}
onMounted(async () => {
  if (!userInfo.user) {
    return;
  }
  if (isSuperAdmin.value) {
    await handleFetchTenantList();
  } else {
    await handleFetchUserTenantList();
  }
});
</script>

<template>
  <NSelect
    v-if="showTenantSelect"
    v-model:value="tenantId"
    :clearable="clearable"
    placeholder="请选择租户"
    :options="tenantOption"
    :loading="loading"
    @update:value="handleChangeTenant"
    @clear="handleClearTenant"
  />
</template>
