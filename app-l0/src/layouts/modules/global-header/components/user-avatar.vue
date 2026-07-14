<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useBoolean, useLoading } from '@sa/hooks';
import { fetchLeaveTenantById, fetchTenantList } from '@/service/api';
import { fetchChangeTenant, fetchClearTenant } from '@/service/api/system/tenant';
import { fetchGetUserTenantList, fetchSwitchUserTenant } from '@/service/api/system/user';
import { useAppStore } from '@/store/modules/app';
import { useAuthStore } from '@/store/modules/auth';
import { useTabStore } from '@/store/modules/tab';
import { useRouterPush } from '@/hooks/common/router';
import defaultAvatar from '@/assets/brand/brand-logo-mark-transparent.png';
import { $t } from '@/locales';

defineOptions({
  name: 'UserAvatar'
});

type TenantOption = {
  tenantId: CommonType.IdType;
  companyName: string;
  domain?: string;
  packageName?: string;
  roleNames?: string[];
  current?: boolean;
  joined?: boolean;
};

const appStore = useAppStore();
const authStore = useAuthStore();
const tabStore = useTabStore();
const { routerPushByKey, toHome, toLogin } = useRouterPush();

const { bool: avatarError, setTrue: setError, setFalse: clearError } = useBoolean(false);
const { loading, startLoading, endLoading } = useLoading();

const popoverShow = ref(false);
const actionTenantId = ref<CommonType.IdType>();
const tenantEnabled = ref(false);
const tenantOptions = ref<TenantOption[]>([]);
const currentTenantId = ref<CommonType.IdType>();

const isSuperAdmin = computed(() => authStore.userInfo.user?.userId === 1);

const displayName = computed(() => {
  const rawName = authStore.userInfo.user?.nickName || authStore.userInfo.user?.userName || '';

  return maskPhone(rawName.trim());
});

const currentTenant = computed(() => {
  return (
    tenantOptions.value.find(item => item.current) ||
    tenantOptions.value.find(item => item.tenantId === currentTenantId.value)
  );
});

const companyName = computed(() => authStore.userInfo.companyName || currentTenant.value?.companyName || '');

const packageName = computed(() => authStore.userInfo.packageName || '');

const currentRoleNames = computed(() => {
  return authStore.userInfo.user?.roles?.map(role => role.roleName).filter(Boolean) || [];
});

const packageTagColor = {
  color: 'rgba(111, 77, 232, 0.14)',
  textColor: '#5b48df',
  borderColor: 'transparent'
};

const otherTenants = computed(() => {
  return tenantOptions.value.filter(item => item.tenantId !== currentTenant.value?.tenantId);
});

const showTenantSection = computed(() => tenantEnabled.value && tenantOptions.value.length > 0);

function maskPhone(phone: string) {
  return phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2');
}

function getTenantMeta(tenant: TenantOption) {
  const displayPackageName = tenant.packageName || (tenant.current ? packageName.value : '');
  const displayRoleNames = tenant.roleNames?.length ? tenant.roleNames : tenant.current ? currentRoleNames.value : [];
  const parts = [displayPackageName, displayRoleNames.join('、')].filter(Boolean);

  return parts.join(' · ') || tenant.domain || '企业成员';
}

function loginOrRegister() {
  toLogin();
}

function handleAvatarLoad() {
  clearError();
}

function handleAvatarError() {
  setError();
}

async function refreshAndClose(message: string) {
  popoverShow.value = false;
  window.$message?.success(message);
  tabStore.clearTabs([], true);
  toHome();
  appStore.reloadPage(500);
}

async function switchTenant(tenant: TenantOption) {
  actionTenantId.value = undefined;
  if (tenant.current || tenant.tenantId === currentTenantId.value) {
    return;
  }

  if (isSuperAdmin.value) {
    if (tenant.tenantId === '000000') {
      await fetchClearTenant();
    } else {
      await fetchChangeTenant(tenant.tenantId);
    }
  } else {
    const { data, error } = await fetchSwitchUserTenant(tenant.tenantId);
    if (error || !data) {
      return;
    }
    const switched = await authStore.loginByToken(data);
    if (!switched) {
      return;
    }
  }

  await refreshAndClose('切换企业成功');
}

function confirmLeaveTenant(tenant: TenantOption) {
  actionTenantId.value = undefined;
  window.$dialog?.warning({
    title: '退出企业',
    content: `确认退出“${tenant.companyName}”？退出后将移除你在该企业下的角色、岗位等关联关系，不会删除业务数据，也不会删除你的账号。`,
    positiveText: '确认退出',
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await leaveTenant(tenant);
    }
  });
}

async function leaveTenant(tenant: TenantOption) {
  startLoading();
  try {
    const { data, error } = await fetchLeaveTenantById(tenant.tenantId);
    if (error || !data) {
      return;
    }

    if (data.hasTenant && data.loginVo) {
      const switched = await authStore.loginByToken(data.loginVo);
      if (!switched) {
        return;
      }
      await refreshAndClose('已退出企业');
      return;
    }

    if (!data.hasTenant) {
      window.$message?.success('已退出企业');
      await authStore.resetStore();
      return;
    }

    window.$message?.success('已退出企业');
    await fetchTenants();
  } finally {
    endLoading();
  }
}

function logout() {
  popoverShow.value = false;
  window.$dialog?.info({
    title: $t('common.tip'),
    content: $t('common.logoutConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: () => {
      authStore.logout();
    }
  });
}

function goUserCenter() {
  popoverShow.value = false;
  routerPushByKey('user-center');
}

async function fetchSuperAdminTenants() {
  const { data, error } = await fetchTenantList();
  if (error || !data) {
    return;
  }

  tenantEnabled.value = data.tenantEnabled;
  currentTenantId.value = authStore.userInfo.user?.tenantId || '000000';
  tenantOptions.value = data.voList.map(tenant => {
    return {
      tenantId: tenant.tenantId,
      companyName: tenant.companyName || String(tenant.tenantId),
      domain: tenant.domain,
      packageName: tenant.tenantId === currentTenantId.value ? packageName.value : undefined,
      roleNames: tenant.tenantId === currentTenantId.value ? currentRoleNames.value : ['管理员'],
      current: tenant.tenantId === currentTenantId.value,
      joined: true
    };
  });
}

async function fetchUserTenants() {
  const { data, error } = await fetchGetUserTenantList();
  if (error || !data) {
    return;
  }

  tenantEnabled.value = data.length > 0;
  tenantOptions.value = data.map(tenant => {
    if (tenant.current) {
      currentTenantId.value = tenant.tenantId;
    }

    return {
      tenantId: tenant.tenantId,
      companyName: tenant.companyName || String(tenant.tenantId),
      domain: tenant.domain,
      packageName: tenant.packageName,
      roleNames: tenant.roleNames,
      current: tenant.current,
      joined: !tenant.current
    };
  });
}

async function fetchTenants() {
  if (!authStore.userInfo.user) {
    return;
  }

  startLoading();
  try {
    if (isSuperAdmin.value) {
      await fetchSuperAdminTenants();
    } else {
      await fetchUserTenants();
    }
  } finally {
    endLoading();
  }
}

onMounted(fetchTenants);
</script>

<template>
  <NButton v-if="!authStore.isLogin" quaternary @click="loginOrRegister">
    {{ $t('page.login.common.loginOrRegister') }}
  </NButton>

  <NPopover v-else v-model:show="popoverShow" trigger="click" placement="bottom-end" :show-arrow="false" raw>
    <template #trigger>
      <button class="user-trigger" type="button">
        <div class="trigger-main" :class="{ 'opacity-50': avatarError }">
          <NAvatar
            v-if="authStore.userInfo.user?.avatar"
            :size="36"
            round
            :src="authStore.userInfo.user?.avatar"
            @load="handleAvatarLoad"
            @error="handleAvatarError"
          />
          <NAvatar v-else :size="36" round :src="defaultAvatar" @load="handleAvatarLoad" @error="handleAvatarError" />
          <div class="trigger-info">
            <div class="account-row">
              <span class="display-name" :title="displayName">{{ displayName }}</span>
              <NTag
                v-if="packageName"
                size="small"
                round
                :bordered="false"
                :color="packageTagColor"
                class="package-tag"
              >
                <span class="package-name" :title="packageName">{{ packageName }}</span>
              </NTag>
            </div>
            <div v-if="companyName" class="tenant-row">
              <span class="company-name" :title="companyName">{{ companyName }}</span>
            </div>
          </div>
        </div>
        <SvgIcon icon="ph:caret-down-bold" class="trigger-caret" />
      </button>
    </template>

    <div class="tenant-menu">
      <div class="profile-head">
        <div class="avatar-wrap">
          <NAvatar
            v-if="authStore.userInfo.user?.avatar"
            :size="42"
            round
            :src="authStore.userInfo.user?.avatar"
            @load="handleAvatarLoad"
            @error="handleAvatarError"
          />
          <NAvatar v-else :size="42" round :src="defaultAvatar" @load="handleAvatarLoad" @error="handleAvatarError" />
        </div>
        <div class="min-w-0">
          <div class="profile-name truncate" :title="displayName">{{ displayName }}</div>
          <div v-if="currentTenant?.companyName" class="profile-sub truncate" :title="currentTenant.companyName">
            {{ currentTenant.companyName }}
          </div>
        </div>
      </div>

      <NSpin :show="loading">
        <div v-if="showTenantSection" class="tenant-section">
          <div class="section-title">当前所属公司</div>
          <div v-if="currentTenant" class="tenant-item tenant-item-current">
            <div class="tenant-icon current">
              <SvgIcon icon="ph:buildings" />
            </div>
            <div class="tenant-copy">
              <div class="tenant-name-row">
                <span class="tenant-name truncate" :title="currentTenant.companyName">
                  {{ currentTenant.companyName }}
                </span>
                <span class="tenant-badge">当前</span>
              </div>
              <div class="tenant-meta truncate">{{ getTenantMeta(currentTenant) }}</div>
            </div>
          </div>

          <div v-if="otherTenants.length" class="section-title other-section-title">切换其他企业</div>
          <NScrollbar v-if="otherTenants.length" class="tenant-scroll">
            <div
              v-for="tenant in otherTenants"
              :key="tenant.tenantId"
              class="tenant-item"
              role="button"
              tabindex="0"
              @click="switchTenant(tenant)"
              @keyup.enter="switchTenant(tenant)"
            >
              <div class="tenant-icon">
                <SvgIcon icon="ph:building-office" />
              </div>
              <div class="tenant-copy">
                <div class="tenant-name-row">
                  <span class="tenant-name truncate" :title="tenant.companyName">{{ tenant.companyName }}</span>
                  <span class="tenant-badge muted">{{ isSuperAdmin ? '可管理' : '我加入的' }}</span>
                </div>
                <div class="tenant-meta truncate">{{ getTenantMeta(tenant) }}</div>
              </div>
              <NPopover
                :show="actionTenantId === tenant.tenantId"
                trigger="click"
                placement="bottom-end"
                :show-arrow="false"
                raw
                @update:show="show => (actionTenantId = show ? tenant.tenantId : undefined)"
              >
                <template #trigger>
                  <button class="tenant-more-button" type="button" @click.stop>
                    <SvgIcon icon="ph:dots-three-vertical-bold" class="tenant-more" />
                  </button>
                </template>
                <div class="tenant-action-menu" @click.stop>
                  <button class="tenant-action-item" type="button" @click="switchTenant(tenant)">
                    <SvgIcon icon="ph:arrows-left-right-bold" />
                    <span>切换到该企业</span>
                  </button>
                  <button
                    v-if="!isSuperAdmin"
                    class="tenant-action-item danger"
                    type="button"
                    @click="confirmLeaveTenant(tenant)"
                  >
                    <SvgIcon icon="ph:sign-out" />
                    <span>退出企业</span>
                  </button>
                </div>
              </NPopover>
            </div>
          </NScrollbar>
        </div>
      </NSpin>

      <div class="menu-footer">
        <button class="footer-action" type="button" @click="goUserCenter">
          <SvgIcon icon="ph:user-circle" />
          <span>个人中心</span>
        </button>
        <button class="footer-action danger" type="button" @click="logout">
          <SvgIcon icon="ph:sign-out" />
          <span>退出登录</span>
        </button>
      </div>
    </div>
  </NPopover>
</template>

<style lang="scss">
.user-trigger {
  display: inline-flex;
  max-width: min(280px, 44vw);
  align-items: center;
  gap: 6px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  padding: 4px 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background: rgba(15, 23, 42, 0.06);
  }
}

.trigger-main {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.trigger-info {
  min-width: 0;
  max-width: 210px;
  text-align: left;
}

.account-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.display-name {
  min-width: 0;
  max-width: 104px;
  overflow: hidden;
  color: var(--n-text-color-1);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.package-tag {
  flex: 0 1 auto;
  max-width: 96px;
}

.package-tag .n-tag__content {
  min-width: 0;
}

.package-name {
  display: inline-block;
  max-width: 76px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: top;
  white-space: nowrap;
}

.tenant-row {
  margin-top: 1px;
  min-width: 0;
}

.company-name {
  display: block;
  max-width: 210px;
  overflow: hidden;
  color: var(--n-text-color-3);
  font-size: 12px;
  line-height: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trigger-caret {
  flex: 0 0 auto;
  color: #8a94a6;
  font-size: 13px;
}

.tenant-menu {
  width: 340px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: var(--n-color);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.18);
}

.tenant-menu .profile-head {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 22px 22px 20px;
}

.tenant-menu .avatar-wrap {
  position: relative;
  flex: 0 0 auto;
}

.tenant-menu .profile-name {
  color: var(--n-text-color-1);
  font-size: 17px;
  font-weight: 600;
  line-height: 24px;
}

.tenant-menu .profile-sub {
  margin-top: 2px;
  color: var(--n-text-color-3);
  font-size: 12px;
  line-height: 18px;
}

.tenant-menu .tenant-section {
  border-top: 1px solid rgba(226, 232, 240, 0.8);
  padding: 18px 17px 16px;
}

.tenant-menu .section-title {
  margin: 0 0 10px 4px;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.tenant-menu .other-section-title {
  margin-top: 24px;
}

.tenant-menu .tenant-scroll {
  max-height: 190px;
}

.tenant-menu .tenant-item {
  display: flex;
  width: 100%;
  min-height: 66px;
  align-items: center;
  gap: 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  padding: 11px 11px 11px 16px;
  color: inherit;
  cursor: pointer;
  outline: none;
  text-align: left;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;

  &:hover {
    background: rgba(99, 102, 241, 0.06);
  }
}

.tenant-menu .tenant-item-current {
  border-color: rgba(129, 109, 246, 0.34);
  background: rgba(117, 100, 241, 0.06);
  box-shadow: inset 0 0 0 1px rgba(129, 109, 246, 0.08);
  cursor: default;
}

.tenant-menu .tenant-icon {
  display: inline-flex;
  width: 39px;
  height: 39px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 2px;
  background: #eef0f4;
  color: #445066;
  font-size: 22px;

  &.current {
    background: rgba(111, 77, 232, 0.15);
    color: #5b48df;
  }
}

.tenant-menu .tenant-copy {
  min-width: 0;
  flex: 1;
}

.tenant-menu .tenant-name-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.tenant-menu .tenant-name {
  min-width: 0;
  color: var(--n-text-color-1);
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
}

.tenant-menu .tenant-badge {
  flex: 0 0 auto;
  border-radius: 4px;
  background: rgba(111, 77, 232, 0.12);
  padding: 1px 5px;
  color: #5b48df;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;

  &.muted {
    background: rgba(100, 116, 139, 0.1);
    color: #64748b;
  }
}

.tenant-menu .tenant-meta {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.tenant-menu .tenant-more {
  color: #64748b;
  font-size: 21px;
}

.tenant-menu .tenant-more-button {
  display: inline-flex;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background: rgba(100, 116, 139, 0.1);
  }
}

.tenant-action-menu {
  width: 190px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 4px;
  background: var(--n-color);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.16);
}

.tenant-action-item {
  display: flex;
  width: 100%;
  height: 48px;
  align-items: center;
  gap: 11px;
  border: 0;
  background: transparent;
  padding: 0 16px;
  color: var(--n-text-color-1);
  cursor: pointer;
  font-size: 14px;
  text-align: left;
  transition: background-color 0.2s ease;

  &:hover {
    background: rgba(100, 116, 139, 0.08);
  }

  &.danger {
    color: #ff4d4f;
  }
}

.tenant-menu .menu-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  padding: 10px 17px;
}

.tenant-menu .footer-action {
  display: inline-flex;
  height: 38px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s ease;

  &:hover {
    background: rgba(100, 116, 139, 0.08);
  }

  &.danger {
    color: #ff4d4f;
  }
}

@media (max-width: 768px) {
  .user-trigger {
    max-width: 184px;
  }

  .trigger-info {
    max-width: 126px;
  }

  .display-name {
    max-width: 68px;
  }

  .package-tag {
    max-width: 54px;
  }

  .package-name {
    max-width: 38px;
  }

  .company-name {
    max-width: 126px;
  }

  .tenant-menu {
    width: min(340px, calc(100vw - 24px));
  }
}
</style>
