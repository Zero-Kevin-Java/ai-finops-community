/**
 * Namespace Api
 *
 * LLM api types
 */
declare namespace Api {
  namespace Llm {
    /** LLM 模型类型 */
    type ModelType = 'chat' | 'embedding' | 'rerank' | 'image' | 'audio' | string;

    /** LLM 二值标记：0 否，1 是 */
    type BinaryFlag = '0' | '1';

    /** LLM 公共价格模式 */
    type PublicPricingMode = 'tiered' | 'token' | 'request' | 'second' | string;
    type ModelPricingMode = PublicPricingMode;

    /** LLM 模型 */
    type Model = Common.CommonRecord<{
      /** 模型 ID */
      modelId: CommonType.IdType;
      /** 模型编码 */
      modelCode: string;
      /** 展示名称 */
      displayName: string;
      /** 模型厂商 */
      provider: string;
      /** 模型供应商，表示模型的提供来源 */
      supplier: string;
      /** LiteLLM 模型名 */
      litellmModel: string;
      /** 协议类型 */
      protocol: 'openai' | 'anthropic' | string;
      /** API Key（AES-256 加密，前端展示时脱敏） */
      apiKey: string;
      /** 上游 API 端点地址 */
      apiBase: string;
      /** 模型类型 */
      modelType: ModelType;
      /** 状态 */
      status: Common.EnableStatus;
      /** 备注 */
      remark: string;
    }>;

    /** LLM 模型查询参数 */
    type ModelSearchParams = CommonType.RecordNullable<
      Pick<Model, 'modelCode' | 'displayName' | 'provider' | 'supplier' | 'modelType' | 'status'> &
        Api.Common.CommonSearchParams
    >;

    /** LLM 模型操作参数 */
    type ModelOperateParams = CommonType.RecordNullable<
      Pick<
        Model,
        | 'modelId'
        | 'modelCode'
        | 'displayName'
        | 'provider'
        | 'supplier'
        | 'litellmModel'
        | 'protocol'
        | 'apiKey'
        | 'apiBase'
        | 'modelType'
        | 'status'
        | 'remark'
      >
    >;

    /** LLM 模型状态操作参数 */
    type ModelStatusOperateParams = CommonType.RecordNullable<Pick<Model, 'modelId' | 'status'>>;

    /** LLM 模型列表 */
    type ModelList = Api.Common.PaginatingQueryRecord<Model>;

    /** LLM 租户模型下拉选项 */
    type ModelOption = {
      modelId?: CommonType.IdType;
      label: string;
      value: string;
      modelCode?: string;
      displayName?: string;
      provider?: string;
      supplier?: string;
    };

    /** LLM 模型用量统计 */
    type ModelUsageStats = {
      /** 输入 token 总计 */
      promptTokens: number;
      /** 输出 token 总计 */
      completionTokens: number;
      /** token 总计 */
      totalTokens: number;
      /** 调用次数 */
      requestCount: number;
      /** 实际总金额 */
      totalAmount: number;
    };

    /** LLM 模型详情 */
    type ModelDetail = {
      /** 模型基础信息 */
      model: Model;
      /** 用量统计 */
      usageStats: ModelUsageStats;
    };

    /** LLM 厂商 */
    type Provider = Common.CommonRecord<{
      /** 厂商 ID */
      providerId: CommonType.IdType;
      /** 厂商名称 */
      providerName: string;
      /** 厂商 Logo 标识 */
      logoSlug: string;
      /** 模型名称匹配前缀 */
      modelPrefixes: string;
      /** 排序 */
      sortOrder: number;
      /** 状态 */
      status: Common.EnableStatus;
      /** 备注 */
      remark: string;
    }>;

    /** LLM 厂商查询参数 */
    type ProviderSearchParams = CommonType.RecordNullable<
      Pick<Provider, 'providerName' | 'status'> & Api.Common.CommonSearchParams
    >;

    /** LLM 厂商操作参数 */
    type ProviderOperateParams = CommonType.RecordNullable<
      Pick<Provider, 'providerId' | 'providerName' | 'logoSlug' | 'modelPrefixes' | 'sortOrder' | 'status' | 'remark'>
    >;

    /** LLM 厂商状态操作参数 */
    type ProviderStatusOperateParams = CommonType.RecordNullable<Pick<Provider, 'providerId' | 'status'>>;

    /** LLM 厂商列表 */
    type ProviderList = Api.Common.PaginatingQueryRecord<Provider>;

    /** LLM 厂商下拉选项 */
    type ProviderOption = {
      label: string;
      value: CommonType.IdType;
      providerName?: string;
      logoSlug?: string;
      modelPrefixes?: string;
    };

    /** LLM 公共模型 */

    /** LLM 公共模型查询参数 */

    /** LLM 公共模型操作参数 */

    /** LLM 公共模型状态操作参数 */

    /** LLM 公共模型列表 */

    /** LLM 公共模型下拉选项 */

    /** 从公共模型导入参数 */

    /** 从公共模型导入结果 */

    /** LLM 项目 */
    type Project = Common.CommonRecord<{
      /** 项目 ID */
      projectId: CommonType.IdType;
      /** 项目编码 */
      projectCode: string;
      /** 项目名称 */
      projectName: string;
      /** 负责人用户 ID */
      ownerUserId: CommonType.IdType;
      /** 负责人 */
      ownerUserName: string;
      /** 状态 */
      status: Common.EnableStatus;
      /** 备注 */
      remark: string;
    }>;

    /** LLM 项目查询参数 */
    type ProjectSearchParams = CommonType.RecordNullable<
      Pick<Project, 'projectCode' | 'projectName' | 'ownerUserId' | 'status'> & Api.Common.CommonSearchParams
    >;

    /** LLM 项目操作参数 */
    type ProjectOperateParams = CommonType.RecordNullable<
      Pick<Project, 'projectId' | 'projectCode' | 'projectName' | 'ownerUserId' | 'status' | 'remark'>
    >;

    /** LLM 项目状态操作参数 */
    type ProjectStatusOperateParams = CommonType.RecordNullable<Pick<Project, 'projectId' | 'status'>>;

    /** LLM 项目列表 */
    type ProjectList = Api.Common.PaginatingQueryRecord<Project>;

    /** LLM 应用类型 */
    type AppType = 'server' | 'web' | 'mobile' | 'internal' | string;

    /** LLM 应用客户端 */
    type AppClient = Common.CommonRecord<{
      /** 应用客户端 ID */
      clientId: CommonType.IdType;
      /** 项目 ID */
      projectId: CommonType.IdType;
      /** 项目名称 */
      projectName: string;
      /** 应用编码 */
      appCode: string;
      /** 应用名称 */
      appName: string;
      /** 应用类型 */
      appType: AppType;
      /** 状态 */
      status: Common.EnableStatus;
      /** 备注 */
      remark: string;
    }>;

    /** LLM 应用客户端查询参数 */
    type AppClientSearchParams = CommonType.RecordNullable<
      Pick<AppClient, 'projectId' | 'appCode' | 'appName' | 'appType' | 'status'> & Api.Common.CommonSearchParams
    >;

    /** LLM 应用客户端操作参数 */
    type AppClientOperateParams = CommonType.RecordNullable<
      Pick<AppClient, 'clientId' | 'projectId' | 'appCode' | 'appName' | 'appType' | 'status' | 'remark'>
    >;

    /** LLM 应用客户端状态操作参数 */
    type AppClientStatusOperateParams = CommonType.RecordNullable<Pick<AppClient, 'clientId' | 'status'>>;

    /** LLM 应用客户端列表 */
    type AppClientList = Api.Common.PaginatingQueryRecord<AppClient>;

    /** LLM API Key 状态 */
    type KeyStatus = Api.Common.EnableStatus;

    /** LLM API Key */
    type ApiKey = Common.CommonRecord<{
      /** API Key ID */
      keyId: CommonType.IdType;
      /** 应用客户端 ID */
      clientId: CommonType.IdType;
      /** 应用名称 */
      appName: string;
      /** 所属用户 ID */
      ownerUserId: CommonType.IdType;
      /** 所属用户 */
      ownerUserName: string;
      /** Key 名称 */
      keyName: string;
      /** API Key（后端脱敏） */
      keyPrefix: string;
      /** 授权模型编码，逗号分隔 */
      keyScope: string;
      /** 过期时间 */
      expireTime: string;
      /** 最后使用时间 */
      lastUsedTime: string;
      /** 状态 */
      status: KeyStatus;
      /** 备注 */
      remark: string;
    }>;

    /** LLM API Key 查询参数 */
    type ApiKeySearchParams = CommonType.RecordNullable<
      Pick<ApiKey, 'clientId' | 'ownerUserId' | 'keyName' | 'keyPrefix' | 'status'> & Api.Common.CommonSearchParams
    >;

    /** LLM API Key 操作参数 */
    type ApiKeyOperateParams = CommonType.RecordNullable<
      Omit<
        Pick<ApiKey, 'keyId' | 'clientId' | 'ownerUserId' | 'keyName' | 'expireTime' | 'status' | 'remark'>,
        'keyScope'
      > & {
        /** 授权模型编码 */
        keyScope: string[];
      }
    >;

    /** LLM API Key 状态操作参数 */
    type ApiKeyStatusOperateParams = CommonType.RecordNullable<Pick<ApiKey, 'keyId' | 'status'>>;

    /** LLM API Key 列表 */
    type ApiKeyList = Api.Common.PaginatingQueryRecord<ApiKey>;

    /** LLM 计费单位 */
    type BillingUnit = '1k_tokens' | '1m_tokens' | 'request' | string;

    /** LLM 阶梯匹配用量口径 */
    type TierUsageBasis = 'total_tokens' | 'prompt_tokens' | 'completion_tokens' | string;

    /** LLM 模型价格阶梯 */

    /** LLM 企业模型价格 */

    /** LLM 企业模型价格查询参数 */

    /** LLM 企业模型价格操作参数 */

    /** LLM 企业模型价格列表 */

    /** LLM 公共模型价格阶梯 */

    /** LLM 公共模型价格 */

    /** LLM 公共模型价格查询参数 */

    /** LLM 公共模型价格操作参数 */

    /** LLM 公共模型价格列表 */

    /** LLM 配额账户类型 */

    /** LLM 额度对象类型 */

    /** LLM 额度类型 */

    /** LLM 配额重置周期 */

    /** LLM 额度耗尽后的执行动作 */

    /** LLM 配额状态 */

    /** LLM 配额账户 */

    /** LLM 配额账户查询参数 */

    /** LLM 配额账户操作参数 */

    /** LLM 配额账户状态操作参数 */

    /** LLM 配额账户列表 */

    /** LLM 额度总览汇总 */

    /** LLM 额度对象剩余额度占比 */

    /** LLM 额度总览 */

    /** LLM 额度对象选项 */

    /** LLM 额度对象选项查询参数 */

    /** LLM 批量额度分配参数 */

    /** LLM 额度临时调增参数 */

    /** LLM 配额业务类型 */

    /** LLM 配额变动方向 */

    /** LLM 配额流水 */

    /** LLM 配额流水查询参数 */

    /** LLM 配额流水列表 */

    /** LLM 费用明细总览 */

    /** LLM 费用明细总览查询参数 */

    /** LLM 费用明细总览明细行 */

    /** 首页总览查询参数 */
    type HomeOverviewSearchParams = {
      /** 开始时间 */
      beginTime: string;
      /** 结束时间 */
      endTime: string;
      /** 部门 ID，空表示当前租户全部组织 */
      deptId?: CommonType.IdType | null;
      /** 项目 ID */
      projectId?: CommonType.IdType | null;
      /** 应用客户端 ID */
      clientId?: CommonType.IdType | null;
      /** 额度展示币种 */
      quotaCurrency?: 'CNY' | 'USD' | 'TOKEN';
    };

    /** 首页总览 */
    type HomeOverview = {
      moneyStats: HomeMoneyStats;
      tokenSummaries: HomeTokenSummary[];
      tokenRows: Record<'project' | 'app' | 'model' | 'employee', HomeTokenRow[]>;
    };

    /** 首页指标 */
    type HomeMoneyStats = {
      cnyTotalAmount: number;
      usdTotalAmount: number;
      totalTokens: number;
      requestCount: number;
      routingHitRate: number;
    };

    /** 首页 Token 汇总 */
    type HomeTokenSummary = {
      dimension: 'project' | 'app' | 'model' | 'employee';
      title: string;
      totalTokens: number;
      ratio: number;
    };

    /** 首页 Token 行 */
    type HomeTokenRow = {
      dimension: 'project' | 'app' | 'model' | 'employee';
      id: string;
      name: string;
      supplier: string | null;
      department: string | null;
      requestCount: number;
      inputTokens: number;
      outputTokens: number;
      cachedTokens: number;
      reasoningTokens: number;
      totalTokens: number;
      cnyAmount: number;
      usdAmount: number;
    };

    /** LLM 用量记录 */

    /** LLM 用量记录查询参数 */

    /** LLM 用量记录列表 */

    /** LLM 用量记录汇总 */

    /** LLM 请求状态 */

    /** LLM 请求日志 */

    /** LLM 请求日志查询参数 */

    /** LLM 请求日志列表 */

    /** LLM 计费记录 */

    /** LLM 计费记录查询参数 */

    /** LLM 计费记录列表 */

    /** LLM 账单汇总 */

    /** LLM 账单汇总查询参数 */

    /** LLM 账单汇总列表 */

    /** LLM 账单对比记录 */

    /** LLM 账单对比查询参数 */

    /** LLM 账单对比列表 */

    /** LLM 账单对比汇总 */

    /** LLM 账单对比解析预览 */

    /** LLM 业务审计事件 */

    /** LLM 业务审计事件查询参数 */

    /** LLM 业务审计事件列表 */

    /** LLM 缓存匹配模式 */
    type CacheMatchMode = 'EXACT' | 'SEMANTIC' | string;

    /** LLM 缓存配置 BO（填报表单） */
    type CacheConfigBo = {
      configId?: string;
      projectId?: number;
      clientId?: number;
      enabled: string;
      matchMode: CacheMatchMode;
      similarityThreshold?: number;
      ttlSeconds: number;
      maxEntries: number;
    };

    /** LLM 缓存配置 VO（列表展示） */
    type CacheConfigVo = {
      configId: string;
      projectId: string;
      clientId: string;
      projectName: string;
      clientName: string;
      enabled: string;
      matchMode: CacheMatchMode;
      similarityThreshold: number;
      ttlSeconds: number;
      maxEntries: number;
      totalHits: number;
      totalMisses: number;
      estimatedTokensSaved: number;
      createTime: string;
    };

    /** LLM 缓存配置查询参数 */
    type CacheConfigSearchParams = CommonType.RecordNullable<
      Pick<CacheConfigVo, 'projectId' | 'clientId' | 'enabled' | 'matchMode'> & Api.Common.CommonSearchParams
    >;

    /** LLM 缓存配置列表 */
    type CacheConfigList = Api.Common.PaginatingQueryRecord<CacheConfigVo>;

    /** LLM 缓存统计 */
    type CacheStatsVo = {
      totalEntries: number;
      totalHits: number;
      totalMisses: number;
      hitRate: number;
      estimatedTokensSaved: number;
    };

    /** LLM 缓存清空参数 */
    type CachePurgeParams = {
      projectId: number;
      clientId?: number;
    };

    /** LLM 缓存配置作用域查询参数 */
    type CacheConfigScopeParams = {
      projectId: number;
      clientId?: number;
    };

    /** LLM 缓存统计查询参数 */
    type CacheStatsParams = {
      projectId: number;
      clientId?: number;
    };

    /** LLM 缓存条目 VO（列表） */
    type CacheEntryVo = {
      entryId: string;
      projectId: string;
      clientId: string;
      modelCode: string;
      promptHash: string;
      promptText: string;
      hitCount: number;
      lastHitAt: string;
      tokenCount: number;
      createdAt: string;
      expiresAt: string;
      createTime: string;
    };

    /** LLM 缓存条目详情 VO（含 responseText） */
    type CacheEntryDetailVo = CacheEntryVo & {
      responseText: string;
    };

    /** LLM 缓存条目查询参数 */
    type CacheEntrySearchParams = CommonType.RecordNullable<
      Pick<CacheEntryVo, 'projectId' | 'clientId' | 'modelCode' | 'promptHash'> & Api.Common.CommonSearchParams
    >;

    /** LLM 缓存条目列表 */
    type CacheEntryList = Api.Common.PaginatingQueryRecord<CacheEntryVo>;
  }
}
