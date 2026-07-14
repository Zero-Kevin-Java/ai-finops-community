<script setup lang="ts">
import { computed, h, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import type { RouteKey } from '@elegant-router/types';
import type { MenuOption } from 'naive-ui';
import { SimpleScrollbar } from '@sa/materials';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import SvgIcon from '@/components/custom/svg-icon.vue';
import { useMenu } from '../context';

defineOptions({
  name: 'VerticalMenu'
});

const route = useRoute();
const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { selectedKey } = useMenu();

const inverted = computed(() => !themeStore.darkMode && themeStore.sider.inverted);
const modelMarketMenuKey = '__model_market__';
const DEFAULT_MODEL_MARKET_URL = 'https://global.modelmesh.info/model';
const modelMarketUrl = import.meta.env.VITE_MODEL_MARKET_URL || DEFAULT_MODEL_MARKET_URL;
const menuOptions = computed<MenuOption[]>(() => [
  ...routeStore.menus,
  {
    key: modelMarketMenuKey,
    label: () => h('span', { style: { color: '#fff' } }, '算力超市'),
    routeKey: modelMarketMenuKey as RouteKey,
    routePath: modelMarketUrl as App.Global.Menu['routePath'],
    icon: () => h(SvgIcon, { icon: 'lucide:box', style: { color: '#fff', fontSize: '20px' } })
  }
]);

const expandedKeys = ref<string[]>([]);

function updateExpandedKeys() {
  if (appStore.siderCollapse || !selectedKey.value) {
    expandedKeys.value = [];
    return;
  }
  expandedKeys.value = routeStore.getSelectedMenuKeyPath(selectedKey.value);
}

watch(
  () => route.name,
  () => {
    updateExpandedKeys();
  },
  { immediate: true }
);

function handleSelectMenu(key: string) {
  if (key === modelMarketMenuKey) {
    window.open(modelMarketUrl, '_blank', 'noopener,noreferrer');
    return;
  }

  routerPushByKeyWithMetaQuery(key as RouteKey);
}

function getMenuNodeProps(option: MenuOption) {
  return option.key === modelMarketMenuKey ? { class: 'model-market-menu-item' } : {};
}
</script>

<template>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <SimpleScrollbar>
      <NMenu
        v-model:expanded-keys="expandedKeys"
        mode="vertical"
        :value="selectedKey"
        :collapsed="appStore.siderCollapse"
        :collapsed-width="themeStore.sider.collapsedWidth"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :inverted="inverted"
        :indent="18"
        :node-props="getMenuNodeProps"
        @update:value="handleSelectMenu"
      />
    </SimpleScrollbar>
  </Teleport>
</template>

<style scoped>
:deep(.model-market-menu-item .n-menu-item-content::before) {
  background: linear-gradient(95deg, #5d66ff 0%, #9a55f5 56%, #ad5cf5 100%);
}
</style>
