export type ProviderMatcher = (modelCode: string) => string;

export type ModelDraft = {
  modelCode: string;
  displayName: string;
  provider: string;
  modelType: Api.Llm.ModelType;
};

export type SimpleTaskTargetOption = {
  label: string;
  value: string;
};

export type SimpleRoutePayload = {
  originalModel: string;
  targetModel: string;
};

export function normalizeModelCodeSelection(value: string | string[] | null | undefined) {
  const values = Array.isArray(value) ? value : [value];
  return Array.from(new Set(values.map(item => item?.trim() || '').filter(Boolean)));
}

export function buildModelDraft(modelCode: string, matchProvider: ProviderMatcher): ModelDraft {
  const normalizedModelCode = modelCode.trim();

  return {
    modelCode: normalizedModelCode,
    displayName: normalizedModelCode,
    provider: matchProvider(normalizedModelCode),
    modelType: 'chat'
  };
}

export function buildModelDrafts(
  modelCodes: string[],
  matchProvider: ProviderMatcher,
  existingDrafts: ModelDraft[] = []
) {
  const existingDraftMap = new Map(existingDrafts.map(item => [item.modelCode, item]));

  return normalizeModelCodeSelection(modelCodes).map(modelCode => {
    const draft = buildModelDraft(modelCode, matchProvider);
    const existingDraft = existingDraftMap.get(draft.modelCode);

    return {
      ...draft,
      displayName: existingDraft?.displayName ?? draft.displayName
    };
  });
}

export function filterSimpleTaskTargetOptions(
  options: SimpleTaskTargetOption[],
  originalModels: string | string[] | null | undefined
) {
  const originalModelSet = new Set(normalizeModelCodeSelection(originalModels));
  if (!originalModelSet.size) return options;

  return options.filter(option => !originalModelSet.has(option.value));
}

export function buildSimpleRoutePayloads(originalModels: string[], targetModel: string | null | undefined) {
  const normalizedTargetModel = targetModel?.trim();
  if (!normalizedTargetModel) return [];

  return normalizeModelCodeSelection(originalModels)
    .filter(originalModel => originalModel !== normalizedTargetModel)
    .map<SimpleRoutePayload>(originalModel => ({
      originalModel,
      targetModel: normalizedTargetModel
    }));
}
