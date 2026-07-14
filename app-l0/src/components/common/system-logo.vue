<script lang="ts" setup>
import brandLogoMark from '@/assets/brand/brand-logo-mark-transparent.png';

defineOptions({ name: 'SystemLogo' });
</script>

<template>
  <img :src="brandLogoMark" alt="AI-FinOps" class="system-logo" />
</template>

<style scoped>
.system-logo {
  display: block;
  height: 1em;
  width: 1.67em;
  object-fit: contain;
}
</style>
