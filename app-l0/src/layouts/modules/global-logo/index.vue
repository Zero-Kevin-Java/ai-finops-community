<script setup lang="ts">
import brandLogoHorizontal from '@/assets/brand/brand-logo-horizontal-transparent.png';
import { $t } from '@/locales';

defineOptions({
  name: 'GlobalLogo'
});

interface Props {
  /** Whether to show the title */
  showTitle?: boolean;
}

withDefaults(defineProps<Props>(), {
  showTitle: true
});
</script>

<template>
  <RouterLink to="/" class="w-full flex-center nowrap-hidden">
    <img
      v-if="showTitle"
      :src="brandLogoHorizontal"
      :alt="$t('system.title')"
      class="h-38px max-w-190px object-contain"
    />
    <SystemLogo v-else class="text-34px" />
  </RouterLink>
</template>

<style scoped></style>
