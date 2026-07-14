<script setup lang="ts">
import { useRouter } from 'vue-router';
import logoUrl from '@/assets/brand/brand-logo-mark-transparent.png';

const router = useRouter();

function goToTrial() {
  router.push('/gateway/trial');
}
</script>

<template>
  <section class="trial-banner" role="button" tabindex="0" @click="goToTrial" @keydown.enter="goToTrial">
    <div class="trial-card">
      <div class="trial-mark" aria-hidden="true">
        <img :src="logoUrl" alt="" />
      </div>
      <div class="trial-copy">
        <strong>3 分钟验证降本空间，点击一键克隆</strong>
        <p>
          <SvgIcon icon="material-symbols:check-circle-outline-rounded" />
          同步对比原模型和智态推荐模型，亲眼见证省钱证据
        </p>
      </div>
      <NButton type="primary" size="large" class="trial-action">
        立即体验
        <template #icon>
          <SvgIcon icon="material-symbols:arrow-outward-rounded" />
        </template>
      </NButton>
    </div>
  </section>
</template>

<style scoped>
.trial-banner {
  display: block;
  padding: 32px;
  border-radius: 8px;
  background: #e5ddff;
  cursor: pointer;
}

.trial-card {
  min-height: 170px;
  display: grid;
  grid-template-columns: 205px minmax(0, 1fr) auto;
  align-items: center;
  gap: 24px;
  padding: 0 38px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.56);
}

.trial-mark {
  width: 174px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.trial-mark img {
  display: block;
  width: 174px;
  max-width: 100%;
  height: auto;
  object-fit: contain;
}

.trial-copy strong {
  display: block;
  color: #27344d;
  font-size: 25px;
  font-weight: 800;
  line-height: 34px;
}

.trial-copy p {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0 0;
  color: #7c8aa1;
  font-size: 14px;
  font-weight: 700;
}

.trial-copy .svg-icon {
  color: #18bd83;
}

.trial-action {
  min-width: 156px;
  height: 58px;
  --n-border-radius: 6px !important;
  --n-color: #514ee5 !important;
  --n-color-hover: #4745d8 !important;
  --n-color-pressed: #3d3ac9 !important;
  --n-color-focus: #514ee5 !important;
  --n-border: 1px solid #514ee5 !important;
  --n-border-hover: 1px solid #4745d8 !important;
  --n-border-pressed: 1px solid #3d3ac9 !important;
  --n-border-focus: 1px solid #514ee5 !important;
  box-shadow: 0 10px 20px rgba(81, 78, 229, 0.24);
  font-size: 17px;
  font-weight: 800;
}

@media (max-width: 980px) {
  .trial-card {
    grid-template-columns: 1fr;
    justify-items: start;
    padding: 28px;
  }

  .trial-mark {
    width: 150px;
    height: 78px;
  }
}
</style>
