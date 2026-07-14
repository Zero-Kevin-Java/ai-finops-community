import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');

const relationFiles = [
  'src/views/llm/api-key/modules/api-key-search.vue',
  'src/views/llm/api-key/modules/api-key-operate-drawer.vue',
  'src/views/llm/app-client/modules/app-client-search.vue',
  'src/views/llm/app-client/modules/app-client-operate-drawer.vue',
  'src/views/llm/billing-record/modules/billing-record-search.vue',
  'src/views/llm/billing-summary/modules/billing-summary-search.vue',
  'src/views/llm/model-policy/modules/model-policy-search.vue',
  'src/views/llm/model-policy/modules/model-policy-operate-drawer.vue',
  'src/views/llm/model-price/modules/model-price-search.vue',
  'src/views/llm/model-price/modules/model-price-operate-drawer.vue',
  'src/views/llm/request-log/modules/request-log-search.vue'
];

const expectedProjectSelectFiles = relationFiles;
const expectedAppSelectFiles = relationFiles.filter(file => !file.includes('/app-client/'));

const failures = [];

function readProjectFile(file) {
  return readFileSync(resolve(root, file), 'utf8');
}

for (const file of expectedProjectSelectFiles) {
  if (!existsSync(resolve(root, file))) {
    continue;
  }
  const source = readProjectFile(file);
  if (!source.includes('LlmProjectSelect')) {
    failures.push(`${file}: projectId must use LlmProjectSelect`);
  }
}

for (const file of expectedAppSelectFiles) {
  if (!existsSync(resolve(root, file))) {
    continue;
  }
  const source = readProjectFile(file);
  if (!source.includes('LlmAppClientSelect')) {
    failures.push(`${file}: clientId must use LlmAppClientSelect`);
  }
  if (!source.includes(':project-id="model.projectId"')) {
    failures.push(`${file}: app select must be scoped by selected projectId`);
  }
  if (!source.includes('@update:value="model.clientId = null"')) {
    failures.push(`${file}: changing projectId must clear the selected clientId`);
  }
}

for (const file of relationFiles) {
  if (!existsSync(resolve(root, file))) {
    continue;
  }
  const source = readProjectFile(file);
  if (/NInputNumber[\s\S]*v-model:value="model\.(projectId|clientId)"/.test(source)) {
    failures.push(`${file}: relation ID fields must not use NInputNumber`);
  }
}

for (const file of [
  'src/components/custom/llm-project-select.vue',
  'src/components/custom/llm-app-client-select.vue'
]) {
  const source = readProjectFile(file);
  if (!source.includes('fetchGetLlm')) {
    failures.push(`${file}: selector must load options from LLM list API`);
  }
}

const zhLocale = readProjectFile('src/locales/langs/zh-cn.ts');
const enLocale = readProjectFile('src/locales/langs/en-us.ts');

function count(source, text) {
  return source.split(text).length - 1;
}

if (count(zhLocale, "'项目 ID'") !== 1 || count(zhLocale, "'请输入项目 ID'") !== 1) {
  failures.push('src/locales/langs/zh-cn.ts: relation project labels must be 项目/请选择项目');
}
if (zhLocale.includes("'应用客户端 ID'") || zhLocale.includes("'请输入应用客户端 ID'")) {
  failures.push('src/locales/langs/zh-cn.ts: relation app labels must be 应用/请选择应用');
}
if (count(enLocale, "'Project ID'") !== 1 || count(enLocale, "'Please enter Project ID'") !== 1) {
  failures.push('src/locales/langs/en-us.ts: relation project labels must be Project/Please select Project');
}
if (enLocale.includes("'App Client ID'") || enLocale.includes("'Please enter App Client ID'")) {
  failures.push('src/locales/langs/en-us.ts: relation app labels must be App/Please select App');
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log('LLM relation select check passed');
