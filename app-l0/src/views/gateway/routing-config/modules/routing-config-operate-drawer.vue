<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useLoading } from '@sa/hooks';
import { fetchCreateRoutingConfigRule, fetchUpdateRoutingConfigRule } from '@/service/api/gateway';
import { fetchGetLlmApiKeyList, fetchGetLlmAppClientList, fetchGetLlmModelOptions } from '@/service/api/llm';
import { fetchGetDeptTree, fetchGetUserSelect } from '@/service/api/system';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';

defineOptions({
  name: 'RoutingConfigOperateDrawer'
});

interface Props {
  operateType: NaiveUI.TableOperateType;
  rowData?: Api.Gateway.RoutingConfigRule | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { loading, startLoading, endLoading } = useLoading();
const { formRef, validate } = useNaiveForm();
const { createRequiredRule } = useFormRules();

type Model = Api.Gateway.RoutingConfigOperateParams;
type ConditionKey = 'departments' | 'appIds' | 'apiKeyIds' | 'userIds' | 'originalModels' | 'keywords' | 'toolNames';

interface ConditionRow {
  id: number;
  key: ConditionKey | null;
}

interface DeptTreeOption {
  id: string;
  parentId: string;
  label: string;
  weight?: number;
  children?: DeptTreeOption[];
}

type RawDeptTreeNode = Record<string, unknown> & {
  id?: CommonType.IdType;
  parentId?: CommonType.IdType | null;
  label?: string;
  weight?: number;
  children?: RawDeptTreeNode[];
};

const model = ref<Model>(createDefaultModel());
const modelOptions = ref<{ label: string; value: string }[]>([]);
const deptTreeOptions = ref<DeptTreeOption[]>([]);
const userOptions = ref<{ label: string; value: string }[]>([]);
const apiKeyOptions = ref<{ label: string; value: string }[]>([]);
const appOptions = ref<{ label: string; value: string }[]>([]);
const conditionRows = ref<ConditionRow[]>([]);
let conditionRowId = 0;

const title = computed(() => {
  const titles: Record<NaiveUI.TableOperateType, string> = {
    add: '新增路由规则',
    edit: '编辑路由规则'
  };
  return titles[props.operateType];
});

const actionOptions: { label: string; value: Api.Gateway.RoutingActionType }[] = [
  { label: '执行项目分类器', value: 'CLASSIFIER' },
  { label: '不拦截', value: 'ORIGINAL_MODEL' },
  { label: '指定模型', value: 'TARGET_MODEL' },
  { label: '拦截', value: 'DENY' }
];

const conditionLogicOptions: { label: string; value: Api.Gateway.RoutingConditionLogic; description: string }[] = [
  { label: '全部满足', value: 'ALL', description: '已填写的条件都命中，才执行动作' },
  { label: '任一条件', value: 'ANY', description: '已填写的条件任意一个命中，就执行动作' }
];

const innerLogicOptions: { label: string; value: Api.Gateway.RoutingConditionLogic }[] = [
  { label: '任一值', value: 'ANY' },
  { label: '全部值', value: 'ALL' }
];

const conditionOptions: { label: string; value: ConditionKey }[] = [
  { label: '部门', value: 'departments' },
  { label: '应用', value: 'appIds' },
  { label: 'API Key', value: 'apiKeyIds' },
  { label: '个人', value: 'userIds' },
  { label: '原模型', value: 'originalModels' },
  { label: 'Prompt 关键词', value: 'keywords' },
  { label: '调用工具', value: 'toolNames' }
];

const fallbackModeOptions = [
  { label: '回到原模型', value: 'ORIGINAL_MODEL' },
  { label: '使用兜底模型', value: 'TARGET_MODEL' },
  { label: '拦截', value: 'DENY' }
];

const rules = {
  ruleName: [createRequiredRule('请输入规则名称')],
  priority: [createRequiredRule('请输入优先级')],
  actionConfig: [
    {
      trigger: 'change',
      validator: () => validateActionConfig()
    }
  ],
  fallbackConfig: [
    {
      trigger: 'change',
      validator: () => validateFallbackConfig()
    }
  ],
  effectiveStart: [
    {
      trigger: 'change',
      validator: () => validateEffectiveTime()
    }
  ],
  effectiveEnd: [
    {
      trigger: 'change',
      validator: () => validateEffectiveTime()
    }
  ]
};

const conditionLogicDescription = computed(() => {
  return conditionLogicOptions.find(item => item.value === model.value.matchConfig.logic)?.description || '';
});

const canAddConditionRow = computed(() => {
  if (conditionRows.value.some(row => !row.key)) return false;
  return conditionRows.value.filter(row => row.key).length < conditionOptions.length;
});

const addConditionText = computed(() => {
  if (conditionRows.value.some(row => !row.key)) return '请先选择条件';
  if (!canAddConditionRow.value) return '已添加全部条件';
  return '添加新条件行';
});

function createDefaultModel(): Model {
  return {
    ruleId: undefined,
    tenantId: '000000',
    ruleName: '',
    priority: 100,
    matchConfig: {
      logic: 'ALL',
      apiKeyIds: [],
      departments: [],
      userIds: [],
      appIds: [],
      pathMatchType: 'PREFIX',
      originalModels: [],
      keywords: [],
      keywordLogic: 'ANY',
      toolNames: [],
      toolLogic: 'ANY',
      headers: {}
    },
    actionConfig: {
      actionType: 'CLASSIFIER',
      targetModel: null,
      denyReason: null
    },
    fallbackConfig: {
      fallbackMode: 'ORIGINAL_MODEL',
      fallbackModels: [],
      defaultAction: 'ORIGINAL_MODEL'
    },
    executionMode: 'ENFORCE',
    rolloutPercent: 100,
    effectiveStart: null,
    effectiveEnd: null,
    remark: '',
    status: '0'
  };
}

function createConditionRow(key: ConditionKey | null = null): ConditionRow {
  conditionRowId += 1;
  return {
    id: conditionRowId,
    key
  };
}

function getConditionOptions(row: ConditionRow) {
  const usedKeys = new Set(
    conditionRows.value
      .filter(item => item.id !== row.id)
      .map(item => item.key)
      .filter(Boolean)
  );

  return conditionOptions.filter(item => !usedKeys.has(item.value));
}

function conditionHasValue(key: ConditionKey) {
  const matchConfig = model.value.matchConfig;
  return Boolean(matchConfig[key]?.length);
}

function syncConditionRowsFromModel() {
  conditionRows.value = conditionOptions
    .filter(item => conditionHasValue(item.value))
    .map(item => createConditionRow(item.value));
}

function addConditionRow() {
  if (!canAddConditionRow.value) return;
  conditionRows.value.push(createConditionRow());
}

function updateConditionRowKey(row: ConditionRow, value: ConditionKey) {
  if (row.key && row.key !== value) {
    clearConditionValue(row.key);
  }
  row.key = value;
}

function removeConditionRow(row: ConditionRow) {
  if (row.key) {
    clearConditionValue(row.key);
  }
  conditionRows.value = conditionRows.value.filter(item => item.id !== row.id);
}

function clearConditionValue(key: ConditionKey) {
  const matchConfig = model.value.matchConfig;

  switch (key) {
    case 'departments':
      matchConfig.departments = [];
      break;
    case 'appIds':
      matchConfig.appIds = [];
      break;
    case 'apiKeyIds':
      matchConfig.apiKeyIds = [];
      matchConfig.apiKeyId = null;
      break;
    case 'userIds':
      matchConfig.userIds = [];
      break;
    case 'originalModels':
      matchConfig.originalModels = [];
      break;
    case 'keywords':
      matchConfig.keywords = [];
      matchConfig.keywordLogic = 'ANY';
      break;
    case 'toolNames':
      matchConfig.toolNames = [];
      matchConfig.toolLogic = 'ANY';
      break;
    default:
      break;
  }
}

function updateDepartments(value: Array<string | number> | null) {
  model.value.matchConfig.departments = (value || []).map(item => String(item));
}

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();
  conditionRows.value = [];

  if (props.operateType !== 'edit' || !props.rowData) return;

  const matchConfig = {
    ...createDefaultModel().matchConfig,
    ...props.rowData.matchConfig
  };
  const actionConfig = {
    ...props.rowData.actionConfig,
    actionType: props.rowData.actionConfig?.actionType || props.rowData.actionType || 'CLASSIFIER'
  };
  delete actionConfig.simpleModel;
  delete actionConfig.complexModel;

  model.value = {
    ruleId: props.rowData.ruleId,
    tenantId: props.rowData.tenantId || '000000',
    ruleName: props.rowData.ruleName,
    priority: props.rowData.priority,
    matchConfig: {
      ...matchConfig,
      apiKeyIds: matchConfig.apiKeyIds?.length
        ? matchConfig.apiKeyIds
        : matchConfig.apiKeyId
          ? [matchConfig.apiKeyId]
          : [],
      departments: matchConfig.departments?.length
        ? matchConfig.departments
        : matchConfig.teamTag
          ? [matchConfig.teamTag]
          : []
    },
    actionConfig: {
      targetModel: null,
      denyReason: null,
      ...actionConfig,
      actionType: actionConfig.actionType
    },
    fallbackConfig: {
      fallbackMode: 'ORIGINAL_MODEL',
      fallbackModels: [],
      defaultAction: 'ORIGINAL_MODEL',
      ...props.rowData.fallbackConfig
    },
    executionMode: props.rowData.executionMode || 'ENFORCE',
    rolloutPercent: props.rowData.rolloutPercent ?? 100,
    effectiveStart: props.rowData.effectiveStart,
    effectiveEnd: props.rowData.effectiveEnd,
    remark: props.rowData.remark || '',
    status: props.rowData.status || '0'
  };

  syncConditionRowsFromModel();
}

async function loadModelOptions() {
  const { data } = await fetchGetLlmModelOptions();
  modelOptions.value = data || [];
}

async function loadConditionOptions() {
  const [{ data: depts }, { data: users }, { data: apiKeys }, { data: apps }] = await Promise.all([
    fetchGetDeptTree(),
    fetchGetUserSelect(),
    fetchGetLlmApiKeyList({ pageNum: 1, pageSize: 1000, status: '0' }),
    fetchGetLlmAppClientList({ pageNum: 1, pageSize: 1000, status: '0' })
  ]);

  deptTreeOptions.value = normalizeDeptTreeOptions((depts || []) as unknown as RawDeptTreeNode[]);
  userOptions.value = (users || []).map(item => ({
    label: item.nickName ? `${item.nickName} (${item.userName})` : item.userName,
    value: String(item.userId)
  }));
  apiKeyOptions.value = (apiKeys?.rows || []).map(item => ({
    label: item.keyName ? `${item.keyName} (${item.keyPrefix})` : String(item.keyId),
    value: String(item.keyId)
  }));
  appOptions.value = (apps?.rows || []).map(item => ({
    label: item.appName ? `${item.appName} (${item.appCode})` : String(item.clientId),
    value: String(item.clientId)
  }));
}

function normalizeDeptTreeOptions(nodes: RawDeptTreeNode[]): DeptTreeOption[] {
  return nodes.map(node => {
    return {
      id: String(node.id ?? ''),
      parentId: String(node.parentId ?? ''),
      label: String(node.label ?? ''),
      weight: node.weight,
      children: normalizeDeptTreeOptions(node.children || [])
    };
  });
}

function validateActionConfig() {
  const { actionType, targetModel } = model.value.actionConfig;
  if (actionType === 'TARGET_MODEL' && !targetModel) return new Error('指定模型动作必须选择目标模型');
  return true;
}

function validateFallbackConfig() {
  const { fallbackMode, fallbackModels } = model.value.fallbackConfig;
  if (fallbackMode === 'TARGET_MODEL' && !fallbackModels?.length) return new Error('使用兜底模型时至少选择一个模型');
  return true;
}

function validateEffectiveTime() {
  const { effectiveStart, effectiveEnd } = model.value;
  if (effectiveStart && effectiveEnd && new Date(effectiveEnd).getTime() < new Date(effectiveStart).getTime()) {
    return new Error('生效结束时间不能早于生效开始时间');
  }
  return true;
}

async function submit() {
  await validate();
  startLoading();

  try {
    const func = props.operateType === 'add' ? fetchCreateRoutingConfigRule : fetchUpdateRoutingConfigRule;
    const { error } = await func(model.value);
    if (!error) {
      visible.value = false;
      emit('submitted');
    }
  } finally {
    endLoading();
  }
}

watch(visible, () => {
  if (visible.value) {
    handleUpdateModelWhenEdit();
    loadModelOptions();
    loadConditionOptions();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" display-directive="show" :width="860" class="routing-rule-drawer">
    <NDrawerContent :title="title" closable>
      <NForm ref="formRef" :model="model" :rules="rules" label-placement="top" class="routing-rule-form">
        <section class="route-step">
          <h3 class="route-step-title">第一步：基础配置</h3>
          <NGrid responsive="screen" item-responsive :x-gap="24" :y-gap="14">
            <NFormItemGi span="24 m:12" label="规则名称" path="ruleName" class="route-field">
              <NInput v-model:value="model.ruleName" placeholder="请输入规则名称，例如：智能客服合同审查规则" />
            </NFormItemGi>
            <NFormItemGi span="24 m:12" label="优先级" path="priority" class="route-field">
              <NInputNumber v-model:value="model.priority" class="w-full" :min="1" placeholder="1" />
            </NFormItemGi>
            <NFormItemGi span="24 m:12" label="备注信息" path="remark" class="route-field">
              <NInput v-model:value="model.remark" placeholder="请输入备注信息" />
            </NFormItemGi>
            <NFormItemGi span="24 m:12" label="开启状态" path="status" class="route-field">
              <NSwitch v-model:value="model.status" checked-value="0" unchecked-value="1" />
            </NFormItemGi>
          </NGrid>
        </section>

        <section class="route-step">
          <h3 class="route-step-title">第二步：设置命中条件与执行动作</h3>

          <div class="condition-logic-line">
            <span class="logic-copy">当以下条件</span>
            <NTabs v-model:value="model.matchConfig.logic" type="segment" size="small" class="rule-segment-tabs">
              <NTab name="ALL" tab="全部满足" />
              <NTab name="ANY" tab="任一条件" />
            </NTabs>
            <span class="logic-copy">时</span>
            <span class="logic-description">{{ conditionLogicDescription }}</span>
          </div>

          <div class="condition-builder">
            <div v-for="(conditionRow, index) in conditionRows" :key="conditionRow.id" class="condition-row">
              <span class="condition-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <NSelect
                :value="conditionRow.key"
                :options="getConditionOptions(conditionRow)"
                placeholder="选择条件"
                class="condition-name-select"
                @update:value="value => updateConditionRowKey(conditionRow, value as ConditionKey)"
              />

              <template v-if="conditionRow.key === 'departments'">
                <span class="condition-operator">是</span>
                <NTreeSelect
                  :value="model.matchConfig.departments"
                  :options="deptTreeOptions as []"
                  multiple
                  cascade
                  filterable
                  clearable
                  checkable
                  key-field="id"
                  label-field="label"
                  max-tag-count="responsive"
                  placeholder="请选择部门"
                  class="condition-control"
                  @update:value="updateDepartments"
                />
              </template>

              <template v-else-if="conditionRow.key === 'appIds'">
                <span class="condition-operator">是</span>
                <NSelect
                  v-model:value="model.matchConfig.appIds"
                  :options="appOptions"
                  multiple
                  filterable
                  clearable
                  max-tag-count="responsive"
                  placeholder="请选择应用"
                  class="condition-control"
                />
              </template>

              <template v-else-if="conditionRow.key === 'apiKeyIds'">
                <span class="condition-operator">是</span>
                <NSelect
                  v-model:value="model.matchConfig.apiKeyIds"
                  :options="apiKeyOptions"
                  multiple
                  filterable
                  clearable
                  max-tag-count="responsive"
                  placeholder="请选择 API Key"
                  class="condition-control"
                />
              </template>

              <template v-else-if="conditionRow.key === 'userIds'">
                <span class="condition-operator">是</span>
                <NSelect
                  v-model:value="model.matchConfig.userIds"
                  :options="userOptions"
                  multiple
                  filterable
                  clearable
                  max-tag-count="responsive"
                  placeholder="请选择个人"
                  class="condition-control"
                />
              </template>

              <template v-else-if="conditionRow.key === 'originalModels'">
                <span class="condition-operator">是</span>
                <NSelect
                  v-model:value="model.matchConfig.originalModels"
                  :options="modelOptions"
                  multiple
                  filterable
                  tag
                  clearable
                  max-tag-count="responsive"
                  placeholder="请选择或输入原模型"
                  class="condition-control"
                />
              </template>

              <template v-else-if="conditionRow.key === 'keywords'">
                <NTabs
                  v-model:value="model.matchConfig.keywordLogic"
                  type="segment"
                  size="small"
                  class="condition-mini-toggle"
                >
                  <NTab v-for="item in innerLogicOptions" :key="item.value" :name="item.value">
                    {{ item.value === 'ANY' ? '包含' : '同时包含' }}
                  </NTab>
                </NTabs>
                <div class="condition-tags">
                  <NDynamicTags v-model:value="model.matchConfig.keywords" />
                </div>
              </template>

              <template v-else-if="conditionRow.key === 'toolNames'">
                <NTabs
                  v-model:value="model.matchConfig.toolLogic"
                  type="segment"
                  size="small"
                  class="condition-mini-toggle"
                >
                  <NTab v-for="item in innerLogicOptions" :key="item.value" :name="item.value">
                    {{ item.value === 'ANY' ? '包含' : '同时包含' }}
                  </NTab>
                </NTabs>
                <div class="condition-tags">
                  <NDynamicTags v-model:value="model.matchConfig.toolNames" />
                </div>
              </template>

              <template v-else>
                <span class="condition-operator">选择</span>
                <div class="condition-empty-control">请选择条件类型</div>
              </template>

              <NButton
                quaternary
                circle
                size="small"
                class="condition-remove-button"
                @click="removeConditionRow(conditionRow)"
              >
                <template #icon>
                  <SvgIcon icon="material-symbols:close-rounded" />
                </template>
              </NButton>
            </div>

            <button type="button" class="add-condition-row" :disabled="!canAddConditionRow" @click="addConditionRow">
              <SvgIcon icon="material-symbols:add" />
              <span>{{ addConditionText }}</span>
            </button>
          </div>

          <div class="action-area">
            <NFormItem label="命中后，执行" path="actionConfig" class="route-field action-field">
              <NTabs
                v-model:value="model.actionConfig.actionType"
                type="segment"
                size="small"
                class="rule-segment-tabs"
              >
                <NTab v-for="item in actionOptions" :key="item.value" :name="item.value">
                  {{ item.label }}
                </NTab>
              </NTabs>
            </NFormItem>

            <NFormItem
              v-if="model.actionConfig.actionType === 'TARGET_MODEL'"
              label="指定模型"
              class="route-field action-field target-model-field"
            >
              <NSelect
                v-model:value="model.actionConfig.targetModel"
                :options="modelOptions"
                filterable
                tag
                clearable
                placeholder="命中后改写到该模型"
              />
            </NFormItem>

            <NFormItem label="执行方式" path="executionMode" class="route-field action-field">
              <NTabs v-model:value="model.executionMode" type="segment" size="small" class="rule-segment-tabs">
                <NTab name="ENFORCE" tab="生效执行" />
                <NTab name="RECORD_ONLY" tab="仅记录不执行" />
              </NTabs>
            </NFormItem>

            <NFormItem
              v-if="model.actionConfig.actionType === 'DENY'"
              label="拦截原因"
              class="route-field action-field"
            >
              <NInput v-model:value="model.actionConfig.denyReason" placeholder="命中后返回的拦截原因" />
            </NFormItem>

            <NFormItem label="兜底模型" path="fallbackConfig" class="route-field action-field fallback-field">
              <NTabs
                v-model:value="model.fallbackConfig.fallbackMode"
                type="segment"
                size="small"
                class="rule-segment-tabs"
              >
                <NTab v-for="item in fallbackModeOptions" :key="item.value" :name="item.value">
                  {{ item.label }}
                </NTab>
              </NTabs>
            </NFormItem>

            <NFormItem
              v-if="model.fallbackConfig.fallbackMode === 'TARGET_MODEL'"
              label="兜底模型列表"
              class="route-field action-field target-model-field"
            >
              <NSelect
                v-model:value="model.fallbackConfig.fallbackModels"
                :options="modelOptions"
                multiple
                filterable
                tag
                clearable
                max-tag-count="responsive"
                placeholder="目标模型不可用或分类器异常时按顺序尝试"
              />
            </NFormItem>
          </div>
        </section>

        <section class="route-step">
          <h3 class="route-step-title">第三步：生效时间段</h3>
          <NGrid responsive="screen" item-responsive :x-gap="24" :y-gap="14">
            <NFormItemGi span="24 m:12" label="生效开始" path="effectiveStart" class="route-field">
              <NDatePicker
                v-model:formatted-value="model.effectiveStart"
                class="w-full"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                clearable
                placeholder="未填则立即生效"
              />
            </NFormItemGi>
            <NFormItemGi span="24 m:12" label="生效结束" path="effectiveEnd" class="route-field">
              <NDatePicker
                v-model:formatted-value="model.effectiveEnd"
                class="w-full"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                clearable
                placeholder="未填则长期有效"
              />
            </NFormItemGi>
          </NGrid>
        </section>
      </NForm>

      <template #footer>
        <NSpace :size="16">
          <NButton @click="visible = false">取消</NButton>
          <NButton type="primary" :loading="loading" @click="submit">保存</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped lang="scss">
:deep(.routing-rule-drawer .n-drawer-content) {
  --route-primary: #4a47df;
  --route-primary-hover: #5d59ef;
  --route-primary-soft: #ece9ff;
  --route-primary-border: #d7cdfd;
  --route-text: #25324a;
  --route-muted: #8792a5;
  --route-border: #e4eaf2;
  --route-surface: #f6f8fb;
}

:deep(.routing-rule-drawer .n-drawer-header) {
  padding: 20px 24px 18px;
  border-bottom: 1px solid #e8edf5;
}

:deep(.routing-rule-drawer .n-drawer-header__main) {
  font-size: 18px;
  font-weight: 700;
  color: var(--route-text);
}

:deep(.routing-rule-drawer .n-drawer-body-content-wrapper) {
  padding: 0 24px 36px;
}

:deep(.routing-rule-drawer .n-drawer-footer) {
  padding: 16px 24px;
  border-top: 0;
  background: #fff;
}

.routing-rule-form {
  color: var(--route-text);
}

.route-step {
  padding-top: 22px;
}

.route-step + .route-step {
  padding-top: 30px;
}

.route-step-title {
  margin: 0 0 16px;
  color: rgb(var(--primary-color));
  font-size: 18px;
  font-weight: 800;
  line-height: 1.25;
}

.route-field {
  margin-bottom: 0;
}

:deep(.route-field .n-form-item-label) {
  padding: 0 0 6px;
  color: var(--route-text);
  font-size: 14px;
  font-weight: 600;
}

:deep(.route-field .n-input),
:deep(.route-field .n-input-number),
:deep(.route-field .n-base-selection),
:deep(.route-field .n-date-picker) {
  width: 100%;
}

:deep(.route-field .n-input-wrapper),
:deep(.route-field .n-base-selection-label),
:deep(.condition-name-select .n-base-selection-label),
:deep(.condition-control .n-base-selection-label) {
  border-color: var(--route-border);
  border-radius: 3px;
  background: var(--route-surface);
  box-shadow: none;
}

:deep(.route-field .n-input__placeholder),
:deep(.condition-name-select .n-base-selection-placeholder),
:deep(.condition-control .n-base-selection-placeholder),
:deep(.route-field .n-base-selection-placeholder) {
  color: #7f8897;
}

:deep(.routing-rule-form .n-switch.n-switch--active .n-switch__rail) {
  background: rgb(var(--primary-color));
}

.condition-logic-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--route-text);
}

.logic-copy {
  font-size: 15px;
  font-weight: 700;
}

.logic-description {
  color: var(--route-muted);
  font-size: 12px;
}

.rule-segment-tabs,
.condition-mini-toggle {
  display: inline-flex;
  width: auto;
  --n-color-segment: #eef3f9 !important;
  --n-tab-border-radius: 5px !important;
  --n-tab-color-segment: #fff !important;
  --n-tab-font-size: 13px !important;
  --n-tab-text-color: #7f8ca2 !important;
  --n-tab-text-color-active: rgb(var(--primary-color)) !important;
  --n-tab-text-color-hover: rgb(var(--primary-color)) !important;
}

.rule-segment-tabs :deep(.n-tabs-nav),
.condition-mini-toggle :deep(.n-tabs-nav) {
  line-height: 1;
}

.rule-segment-tabs :deep(.n-tabs-rail),
.condition-mini-toggle :deep(.n-tabs-rail) {
  width: auto;
  padding: 2px;
  border: 1px solid #dfe7f1;
  border-radius: 6px;
  background-color: #eef3f9;
}

.rule-segment-tabs :deep(.n-tabs-capsule),
.condition-mini-toggle :deep(.n-tabs-capsule) {
  border-radius: 4px;
  box-shadow: none;
}

.rule-segment-tabs :deep(.n-tabs-tab-wrapper),
.condition-mini-toggle :deep(.n-tabs-tab-wrapper) {
  flex: 0 0 auto;
}

.rule-segment-tabs :deep(.n-tabs-tab),
.condition-mini-toggle :deep(.n-tabs-tab) {
  min-width: 74px;
  height: 26px;
  padding: 0 12px !important;
  font-weight: 600;
  white-space: nowrap;
}

.rule-segment-tabs :deep(.n-tabs-tab.n-tabs-tab--active),
.condition-mini-toggle :deep(.n-tabs-tab.n-tabs-tab--active) {
  font-weight: 700;
}

.condition-mini-toggle {
  --n-tab-font-size: 12px !important;
}

.condition-mini-toggle :deep(.n-tabs-tab) {
  min-width: 54px;
  height: 24px;
  padding: 0 9px !important;
}

.condition-builder {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 760px;
  padding-left: 12px;
}

.condition-row {
  display: grid;
  grid-template-columns: 36px 168px max-content minmax(240px, 360px) 30px;
  align-items: center;
  gap: 8px;
  min-height: 56px;
  padding: 10px;
  border: 1px solid #e3eaf4;
  border-radius: 4px;
  background: #f8fbff;
}

.condition-index {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #ebe8ff;
  color: #6258f5;
  font-size: 12px;
  font-weight: 800;
}

.condition-operator {
  color: #90a0b7;
  font-size: 13px;
  font-weight: 500;
  text-align: center;
}

.condition-name-select {
  min-width: 0;
  width: 100%;
}

.condition-control,
.condition-tags {
  min-width: 0;
  width: 100%;
}

.condition-tags {
  display: flex;
  min-height: 34px;
  align-items: center;
  padding: 3px 8px;
  border: 1px solid var(--route-border);
  border-radius: 2px;
  background: var(--route-surface);
}

.condition-empty-control {
  min-height: 34px;
  padding: 8px 10px;
  border: 1px dashed #dce6f4;
  border-radius: 3px;
  background: #fff;
  color: var(--route-muted);
  font-size: 13px;
  line-height: 16px;
}

:deep(.condition-tags .n-dynamic-tags) {
  width: 100%;
}

:deep(.condition-tags .n-tag),
:deep(.condition-control .n-base-selection-tag-wrapper .n-tag) {
  border-color: var(--route-primary-border);
  border-radius: 12px;
  background: var(--route-primary-soft);
  color: #553fdc;
  font-weight: 700;
}

.condition-mini-toggle {
  align-self: center;
}

.condition-remove-button {
  color: #9aa7ba;
}

.condition-remove-button:hover {
  color: rgb(var(--primary-color));
}

.add-condition-row {
  display: inline-flex;
  height: 40px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 2px dashed #dce6f4;
  border-radius: 4px;
  background: #fff;
  color: #8ca0bb;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.add-condition-row:disabled {
  color: #b2bdcc;
  background: #fbfcfe;
  cursor: not-allowed;
}

.action-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 28px;
}

.action-field {
  max-width: 690px;
}

.target-model-field {
  max-width: 390px;
}

.fallback-field {
  margin-top: 8px;
}

@media (max-width: 720px) {
  :deep(.routing-rule-drawer .n-drawer-header),
  :deep(.routing-rule-drawer .n-drawer-body-content-wrapper),
  :deep(.routing-rule-drawer .n-drawer-footer) {
    padding-inline: 18px;
  }

  .route-step-title {
    font-size: 17px;
  }

  .condition-builder {
    padding-left: 0;
  }

  .condition-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .condition-name-select,
  .condition-operator,
  .condition-mini-toggle,
  .condition-control,
  .condition-tags,
  .condition-empty-control,
  .condition-remove-button {
    grid-column: 2;
  }
}
</style>
