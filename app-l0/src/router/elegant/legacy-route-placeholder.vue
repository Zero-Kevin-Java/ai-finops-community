<script setup lang="ts">
import { NResult } from 'naive-ui';

defineOptions({
  name: 'LegacyRoutePlaceholder'
});
</script>

<template>
  <NResult status="404" title="Page not available" description="This backend menu entry has no matching frontend page." />
</template>
