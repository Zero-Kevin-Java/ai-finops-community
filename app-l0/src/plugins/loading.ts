// @unocss-include
import { getRgb } from '@sa/color';
import brandLogoMark from '@/assets/brand/brand-logo-mark-transparent.png';
import { DARK_CLASS } from '@/constants/app';
import { localStg } from '@/utils/storage';
import { toggleHtmlClass } from '@/utils/common';
import { $t } from '@/locales';
import '@/styles/scss/loading.scss';

export function setupLoading() {
  const app = document.getElementById('app');

  const themeColor = localStg.get('themeColor') || '#2080f0';
  const darkMode = localStg.get('darkMode') || false;
  const { r, g, b } = getRgb(themeColor);

  const primaryColor = `--primary-color: ${r} ${g} ${b}`;

  if (darkMode) {
    toggleHtmlClass(DARK_CLASS).add();
  }

  const loading = `
<div class="fixed-center flex-col bg-layout" style="${primaryColor}">
  <div class="my-52px">
    <div class="afo-logo-loader" role="status" aria-label="${$t('system.title')} loading">
      <span class="afo-logo-loader__halo"></span>
      <img class="afo-logo-loader__mark" src="${brandLogoMark}" alt="${$t('system.title')}" />
      <span class="afo-logo-loader__ring"></span>
    </div>
  </div>
  <h2 class="text-30px text-primary-400 font-500">${$t('system.title')}</h2>
</div>`;

  if (app) {
    app.innerHTML = loading;
  }
}
