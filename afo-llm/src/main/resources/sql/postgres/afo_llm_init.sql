-- ----------------------------
-- AI-FinOps LiteLLM 核心表结构与初始化数据
-- 数据库: PostgreSQL
-- 说明:
-- 1. LiteLLM 只负责模型网关、provider 路由和标准化 usage 返回。
-- 2. Spring Boot 负责应用/API Key、模型策略、企业价格、额度、审计和最终计费。
-- 3. 本脚本保持幂等，适合在已有 RuoYi-Vue-Plus 初始化库上增量执行。
-- ----------------------------

create extension if not exists btree_gist;

-- ----------------------------
-- 1、LLM 租户业务档案
-- ----------------------------
create table if not exists afo_llm_tenant
(
    id               int8,
    tenant_id        varchar(20)  default '000000'::varchar,
    tenant_code      varchar(64)  not null,
    tenant_name      varchar(100) not null,
    contact_name     varchar(50)  default null::varchar,
    contact_phone    varchar(30)  default null::varchar,
    billing_currency varchar(10)  default 'USD'::varchar,
    quota_enabled    char         default '1'::bpchar,
    status           char         default '0'::bpchar,
    create_dept      int8,
    create_by        int8,
    create_time      timestamp,
    update_by        int8,
    update_time      timestamp,
    del_flag         char         default '0'::bpchar,
    remark           varchar(500) default null::varchar,
    constraint pk_afo_llm_tenant primary key (id)
);

comment on table afo_llm_tenant is 'LLM 租户业务档案表';
comment on column afo_llm_tenant.id is '主键';
comment on column afo_llm_tenant.tenant_id is '系统租户编号';
comment on column afo_llm_tenant.tenant_code is 'LLM 业务租户编码';
comment on column afo_llm_tenant.tenant_name is 'LLM 业务租户名称';
comment on column afo_llm_tenant.contact_name is '联系人姓名';
comment on column afo_llm_tenant.contact_phone is '联系人电话';
comment on column afo_llm_tenant.billing_currency is '默认计费币种';
comment on column afo_llm_tenant.quota_enabled is '是否启用额度控制（0否 1是）';
comment on column afo_llm_tenant.status is '状态（0正常 1停用）';
comment on column afo_llm_tenant.create_dept is '创建部门';
comment on column afo_llm_tenant.create_by is '创建者';
comment on column afo_llm_tenant.create_time is '创建时间';
comment on column afo_llm_tenant.update_by is '更新者';
comment on column afo_llm_tenant.update_time is '更新时间';
comment on column afo_llm_tenant.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_tenant.remark is '备注';

create unique index if not exists uk_afo_llm_tenant_tenant_id on afo_llm_tenant (tenant_id) where del_flag = '0';
comment on index uk_afo_llm_tenant_tenant_id is '唯一约束：未删除数据中一个系统租户只对应一条 LLM 业务档案';
create unique index if not exists uk_afo_llm_tenant_code on afo_llm_tenant (tenant_code) where del_flag = '0';
comment on index uk_afo_llm_tenant_code is '唯一约束：未删除数据中 LLM 业务租户编码唯一';
create index if not exists idx_afo_llm_tenant_status on afo_llm_tenant (status, del_flag);
comment on index idx_afo_llm_tenant_status is '查询场景：后台按状态筛选启用或停用的 LLM 租户';

-- ----------------------------
-- 2、LLM 项目
-- ----------------------------
create table if not exists afo_llm_project
(
    project_id   int8,
    tenant_id    varchar(20)  default '000000'::varchar,
    project_code varchar(64)  not null,
    project_name varchar(100) not null,
    owner_user_id int8        default null,
    status       char         default '0'::bpchar,
    create_dept  int8,
    create_by    int8,
    create_time  timestamp,
    update_by    int8,
    update_time  timestamp,
    del_flag     char         default '0'::bpchar,
    remark       varchar(500) default null::varchar,
    constraint pk_afo_llm_project primary key (project_id)
);

comment on table afo_llm_project is 'LLM 项目表';
comment on column afo_llm_project.project_id is '项目ID';
comment on column afo_llm_project.tenant_id is '系统租户编号';
comment on column afo_llm_project.project_code is '项目编码';
comment on column afo_llm_project.project_name is '项目名称';
comment on column afo_llm_project.owner_user_id is '项目负责人用户ID';
comment on column afo_llm_project.status is '状态（0正常 1停用）';
comment on column afo_llm_project.create_dept is '创建部门';
comment on column afo_llm_project.create_by is '创建者';
comment on column afo_llm_project.create_time is '创建时间';
comment on column afo_llm_project.update_by is '更新者';
comment on column afo_llm_project.update_time is '更新时间';
comment on column afo_llm_project.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_project.remark is '备注';

create unique index if not exists uk_afo_llm_project_code on afo_llm_project (tenant_id, project_code) where del_flag = '0';
comment on index uk_afo_llm_project_code is '唯一约束：未删除数据中同一租户下项目编码唯一';
create index if not exists idx_afo_llm_project_tenant_status on afo_llm_project (tenant_id, status, del_flag);
comment on index idx_afo_llm_project_tenant_status is '查询场景：后台按租户和状态分页查询项目列表';

-- ----------------------------
-- 3、LLM 应用客户端
-- ----------------------------
create table if not exists afo_llm_app_client
(
    client_id       int8,
    tenant_id       varchar(20)   default '000000'::varchar,
    project_id      int8          not null,
    app_code        varchar(64)   not null,
    app_name        varchar(100)  not null,
    app_type        varchar(30)   default 'server'::varchar,
    status          char          default '0'::bpchar,
    create_dept     int8,
    create_by       int8,
    create_time     timestamp,
    update_by       int8,
    update_time     timestamp,
    del_flag        char          default '0'::bpchar,
    remark          varchar(500)  default null::varchar,
    constraint pk_afo_llm_app_client primary key (client_id)
);

comment on table afo_llm_app_client is 'LLM 应用客户端表';
comment on column afo_llm_app_client.client_id is '应用客户端ID';
comment on column afo_llm_app_client.tenant_id is '系统租户编号';
comment on column afo_llm_app_client.project_id is '项目ID';
comment on column afo_llm_app_client.app_code is '应用编码';
comment on column afo_llm_app_client.app_name is '应用名称';
comment on column afo_llm_app_client.app_type is '应用类型（server/web/mobile/internal）';
comment on column afo_llm_app_client.status is '状态（0正常 1停用）';
comment on column afo_llm_app_client.create_dept is '创建部门';
comment on column afo_llm_app_client.create_by is '创建者';
comment on column afo_llm_app_client.create_time is '创建时间';
comment on column afo_llm_app_client.update_by is '更新者';
comment on column afo_llm_app_client.update_time is '更新时间';
comment on column afo_llm_app_client.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_app_client.remark is '备注';

create unique index if not exists uk_afo_llm_app_client_code on afo_llm_app_client (tenant_id, app_code) where del_flag = '0';
comment on index uk_afo_llm_app_client_code is '唯一约束：未删除数据中同一租户下应用编码唯一';
create index if not exists idx_afo_llm_app_client_project_status on afo_llm_app_client (tenant_id, project_id, status, del_flag);
comment on index idx_afo_llm_app_client_project_status is '查询场景：按项目和状态分页查询应用客户端';

-- ----------------------------
-- 4、LLM 业务 API Key
-- ----------------------------
create table if not exists afo_llm_api_key
(
    key_id         int8,
    tenant_id      varchar(20)  default '000000'::varchar,
    client_id      int8         not null,
    owner_user_id  int8         not null,
    key_name       varchar(100) not null,
    key_prefix     varchar(32)  not null,
    key_hash       varchar(128) not null,
    key_scope      varchar(500) default null::varchar,
    expire_time    timestamp    default null,
    last_used_time timestamp    default null,
    status         char         default '0'::bpchar,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    del_flag       char         default '0'::bpchar,
    remark         varchar(500) default null::varchar,
    constraint pk_afo_llm_api_key primary key (key_id)
);

comment on table afo_llm_api_key is 'LLM 业务 API Key 表';
comment on column afo_llm_api_key.key_id is 'API Key ID';
comment on column afo_llm_api_key.tenant_id is '系统租户编号';
comment on column afo_llm_api_key.client_id is '应用客户端ID';
comment on column afo_llm_api_key.owner_user_id is '所属用户ID';
comment on column afo_llm_api_key.key_name is 'Key 名称';
comment on column afo_llm_api_key.key_prefix is 'API Key 定位前缀，用于检索，不对外明文返回';
comment on column afo_llm_api_key.key_hash is 'Key 哈希值，不存储明文';
comment on column afo_llm_api_key.key_scope is '授权范围，多个用逗号分隔';
comment on column afo_llm_api_key.expire_time is '过期时间，空值表示永不过期';
comment on column afo_llm_api_key.last_used_time is '最后使用时间';
comment on column afo_llm_api_key.status is '状态（0正常 1停用）';
comment on column afo_llm_api_key.create_dept is '创建部门';
comment on column afo_llm_api_key.create_by is '创建者';
comment on column afo_llm_api_key.create_time is '创建时间';
comment on column afo_llm_api_key.update_by is '更新者';
comment on column afo_llm_api_key.update_time is '更新时间';
comment on column afo_llm_api_key.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_api_key.remark is '备注';

create unique index if not exists uk_afo_llm_api_key_prefix on afo_llm_api_key (key_prefix);
comment on index uk_afo_llm_api_key_prefix is '唯一约束：API Key 定位前缀唯一，用于日志定位';
create unique index if not exists uk_afo_llm_api_key_hash on afo_llm_api_key (key_hash);
comment on index uk_afo_llm_api_key_hash is '唯一约束：Key 哈希唯一，用于鉴权查找';
create index if not exists idx_afo_llm_api_key_client_status on afo_llm_api_key (tenant_id, client_id, status, del_flag);
comment on index idx_afo_llm_api_key_client_status is '查询场景：按应用客户端和状态分页查询 API Key';
create index if not exists idx_afo_llm_api_key_owner_status on afo_llm_api_key (tenant_id, owner_user_id, status, del_flag);
comment on index idx_afo_llm_api_key_owner_status is '查询场景：按所属用户和状态分页查询 API Key';

-- ----------------------------
-- 5、LLM 模型目录
-- ----------------------------
create table if not exists afo_llm_model_catalog
(
    model_id          int8,
    tenant_id         varchar(20)  default '000000'::varchar,
    model_code        varchar(100) not null,
    display_name      varchar(100) not null,
    provider          varchar(50)  not null,
    supplier          varchar(100) default null::varchar,
    litellm_model     varchar(150) not null,
    model_type        varchar(30)  default 'chat'::varchar,
    context_window                  int4         default null,
    supports_stream                 char         default '1'::bpchar,
    supports_tool                   char         default '0'::bpchar,
    protocol                        varchar(20)  not null default 'openai',
    api_key                         text,
    api_base                        varchar(500),
    supports_vision                 char         default '0'::bpchar,
    supports_parallel_function_calling char      default '0'::bpchar,
    supports_reasoning              char         default '0'::bpchar,
    max_input_tokens                int4,
    max_output_tokens               int4,
    api_version                     varchar(50),
    rate_limit_tpm                  int4,
    rate_limit_rpm                  int4,
    request_timeout                 int4,
    model_tags                      text,
    routing_order                   int4,
    status                          char         default '0'::bpchar,
    sync_status                     varchar(20)  default 'synced'::varchar,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    del_flag          char         default '0'::bpchar,
    remark            varchar(500) default null::varchar,
    constraint pk_afo_llm_model_catalog primary key (model_id)
);

comment on table afo_llm_model_catalog is 'LLM 模型目录表';
comment on column afo_llm_model_catalog.model_id is '模型ID';
comment on column afo_llm_model_catalog.tenant_id is '系统租户编号';
comment on column afo_llm_model_catalog.model_code is '平台内部模型编码';
comment on column afo_llm_model_catalog.display_name is '模型展示名称';
comment on column afo_llm_model_catalog.provider is '模型厂商';
comment on column afo_llm_model_catalog.supplier is '供应商，表示模型的提供来源';
comment on column afo_llm_model_catalog.litellm_model is 'LiteLLM 模型名称';
comment on column afo_llm_model_catalog.model_type is '模型类型';
comment on column afo_llm_model_catalog.context_window is '上下文窗口 token 数';
comment on column afo_llm_model_catalog.supports_stream is '是否支持流式输出（0否 1是）';
comment on column afo_llm_model_catalog.supports_tool is '是否支持工具调用（0否 1是）';
comment on column afo_llm_model_catalog.protocol is '协议类型: openai / anthropic';
comment on column afo_llm_model_catalog.api_key is 'API Key（AES-256/GCM 加密存储）';
comment on column afo_llm_model_catalog.api_base is '上游 API 端点地址';
comment on column afo_llm_model_catalog.supports_vision is '是否支持视觉（0否 1是）';
comment on column afo_llm_model_catalog.supports_parallel_function_calling is '是否支持并行函数调用（0否 1是）';
comment on column afo_llm_model_catalog.supports_reasoning is '是否支持推理（0否 1是）';
comment on column afo_llm_model_catalog.max_input_tokens is '最大输入 token 数';
comment on column afo_llm_model_catalog.max_output_tokens is '最大输出 token 数';
comment on column afo_llm_model_catalog.api_version is 'API 版本';
comment on column afo_llm_model_catalog.rate_limit_tpm is 'TPM 限制';
comment on column afo_llm_model_catalog.rate_limit_rpm is 'RPM 限制';
comment on column afo_llm_model_catalog.request_timeout is '请求超时（秒）';
comment on column afo_llm_model_catalog.model_tags is '模型标签（JSON 数组，如 ["vision","fast"]）';
comment on column afo_llm_model_catalog.routing_order is '路由优先级（越低越优先，L2 使用）';
comment on column afo_llm_model_catalog.sync_status is '同步状态（synced/pending/failed）';
comment on column afo_llm_model_catalog.status is '状态（0正常 1停用）';
comment on column afo_llm_model_catalog.create_dept is '创建部门';
comment on column afo_llm_model_catalog.create_by is '创建者';
comment on column afo_llm_model_catalog.create_time is '创建时间';
comment on column afo_llm_model_catalog.update_by is '更新者';
comment on column afo_llm_model_catalog.update_time is '更新时间';
comment on column afo_llm_model_catalog.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_model_catalog.remark is '备注';

create unique index if not exists uk_afo_llm_model_catalog_code on afo_llm_model_catalog (tenant_id, model_code) where del_flag = '0';
comment on index uk_afo_llm_model_catalog_code is '唯一约束：未删除数据中同一租户下模型编码唯一';
create index if not exists idx_afo_llm_model_catalog_type_status on afo_llm_model_catalog (tenant_id, model_type, status, del_flag);
comment on index idx_afo_llm_model_catalog_type_status is '查询场景：按模型类型和状态筛选可用模型';

-- ----------------------------
-- 6、LLM 企业模型价格
-- ----------------------------
create table if not exists afo_llm_customer_model_price
(
    price_id               int8,
    tenant_id              varchar(20)    default '000000'::varchar,
    model_code             varchar(100)   not null,
    currency               varchar(10)    default 'USD'::varchar,
    billing_unit           varchar(30)    default '1k_tokens'::varchar,
    pricing_mode           varchar(20)    default 'token'::varchar,
    prompt_price           numeric(20, 8) default 0,
    completion_price       numeric(20, 8) default 0,
    request_price          numeric(20, 8) default 0,
    second_price           numeric(20, 8) default 0,
    create_dept            int8,
    create_by              int8,
    create_time            timestamp,
    update_by              int8,
    update_time            timestamp,
    del_flag               char           default '0'::bpchar,
    remark                 varchar(500)   default null::varchar,
    constraint pk_afo_llm_customer_model_price primary key (price_id)
);

comment on table afo_llm_customer_model_price is 'LLM 企业模型价格表';
comment on column afo_llm_customer_model_price.price_id is '价格ID';
comment on column afo_llm_customer_model_price.tenant_id is '系统租户编号';
comment on column afo_llm_customer_model_price.model_code is '模型编码';
comment on column afo_llm_customer_model_price.currency is '币种';
comment on column afo_llm_customer_model_price.billing_unit is '计费单位';
comment on column afo_llm_customer_model_price.pricing_mode is '计价类型（tiered/token/request/second）';
comment on column afo_llm_customer_model_price.prompt_price is '输入 token 单价';
comment on column afo_llm_customer_model_price.completion_price is '输出 token 单价';
comment on column afo_llm_customer_model_price.request_price is '请求单价';
comment on column afo_llm_customer_model_price.second_price is '秒级单价';
comment on column afo_llm_customer_model_price.create_dept is '创建部门';
comment on column afo_llm_customer_model_price.create_by is '创建者';
comment on column afo_llm_customer_model_price.create_time is '创建时间';
comment on column afo_llm_customer_model_price.update_by is '更新者';
comment on column afo_llm_customer_model_price.update_time is '更新时间';
comment on column afo_llm_customer_model_price.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_customer_model_price.remark is '备注';

do $$
begin
    alter table afo_llm_customer_model_price
        drop constraint if exists ex_afo_llm_price_effective;

    alter table afo_llm_customer_model_price
        add column if not exists pricing_mode varchar(20) default 'token'::varchar,
        add column if not exists request_price numeric(20, 8) default 0,
        add column if not exists second_price numeric(20, 8) default 0;

    update afo_llm_customer_model_price
    set pricing_mode = 'token'
    where pricing_mode is null;

    alter table afo_llm_customer_model_price
        drop column if exists project_id,
        drop column if exists client_id,
        drop column if exists version_no,
        drop column if exists effective_start,
        drop column if exists effective_end,
        drop column if exists status;

    drop index if exists idx_afo_llm_price_lookup;
    drop index if exists idx_afo_llm_price_model;
end $$;

create index if not exists idx_afo_llm_price_lookup on afo_llm_customer_model_price (tenant_id, model_code, currency, pricing_mode, del_flag);
comment on index idx_afo_llm_price_lookup is '查询场景：按租户、模型、币种、计价类型匹配企业价格';
create index if not exists idx_afo_llm_price_model on afo_llm_customer_model_price (tenant_id, model_code, currency, pricing_mode, del_flag);
comment on index idx_afo_llm_price_model is '查询场景：后台按模型和币种分页查询价格配置';
create index if not exists idx_afo_llm_price_model_history on afo_llm_customer_model_price (tenant_id, model_code, update_time desc, create_time desc) where del_flag = '0';
comment on index idx_afo_llm_price_model_history is '查询场景：模型详情按模型查询价格历史';

-- ----------------------------
-- 8、LLM 模型阶梯价格
-- ----------------------------
create table if not exists afo_llm_model_price_tier
(
    tier_id                int8,
    price_id               int8           not null,
    tier_no                int4           default 1,
    min_volume             int8           not null default 0,
    max_volume             int8           default null,
    usage_basis            varchar(30)    default 'prompt_tokens'::varchar,
    prompt_price           numeric(20, 8) default 0,
    completion_price       numeric(20, 8) default 0,
    request_price          numeric(20, 8) default 0,
    second_price           numeric(20, 8) default 0,
    create_dept            int8,
    create_by              int8,
    create_time            timestamp,
    update_by              int8,
    update_time            timestamp,
    del_flag               char           default '0'::bpchar,
    remark                 varchar(500)   default null::varchar,
    constraint pk_afo_llm_model_price_tier primary key (tier_id)
);

comment on table afo_llm_model_price_tier is '模型阶梯价格';
comment on column afo_llm_model_price_tier.tier_id is '阶梯ID';
comment on column afo_llm_model_price_tier.price_id is '价格ID';
comment on column afo_llm_model_price_tier.tier_no is '阶梯序号';
comment on column afo_llm_model_price_tier.min_volume is '起始用量';
comment on column afo_llm_model_price_tier.max_volume is '截止用量（null 无上限）';
comment on column afo_llm_model_price_tier.usage_basis is '阶梯匹配用量口径：total_tokens 总用量，prompt_tokens 输入用量，completion_tokens 输出用量';
comment on column afo_llm_model_price_tier.prompt_price is '输入单价';
comment on column afo_llm_model_price_tier.completion_price is '输出单价';
comment on column afo_llm_model_price_tier.request_price is '请求单价';
comment on column afo_llm_model_price_tier.second_price is '秒级单价';
comment on column afo_llm_model_price_tier.remark is '备注';

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'fk_price_tier_price_id'
    ) then
        alter table afo_llm_model_price_tier
            add constraint fk_price_tier_price_id
            foreign key (price_id) references afo_llm_customer_model_price(price_id)
            on delete cascade;
    end if;
end $$;

create index if not exists idx_price_tier_price_id on afo_llm_model_price_tier(price_id);
create unique index if not exists uk_afo_llm_model_price_tier_no on afo_llm_model_price_tier (price_id, tier_no);
create index if not exists idx_afo_llm_model_price_tier_volume on afo_llm_model_price_tier (price_id, min_volume, max_volume);

-- ----------------------------
-- 9、LLM 配额账户
-- ----------------------------
create table if not exists afo_llm_quota_account
(
    account_id     int8,
    tenant_id      varchar(20)    default '000000'::varchar,
    project_id     int8           default null,
    client_id      int8           default null,
    object_id      int8           default null,
    account_type   varchar(30)    default 'tenant'::varchar,
    quota_unit     varchar(20)    default 'amount'::varchar,
    currency       varchar(10)    default 'USD'::varchar,
    quota_amount   numeric(20, 6) default 0,
    used_amount    numeric(20, 6) default 0,
    balance_amount numeric(20, 6) default 0,
    frozen_amount  numeric(20, 6) default 0,
    reset_cycle    varchar(20)    default 'none'::varchar,
    reset_time     timestamp      default null,
    previous_account_id int8      default null,
    active_flag    char           default '1'::bpchar,
    reset_clock    time           default null,
    reset_day      int4           default null,
    effective_time timestamp      default null,
    limit_action   varchar(20)    default 'continue'::varchar,
    warning_enabled char          default '1'::bpchar,
    warning_threshold numeric(5, 2) default 85.00,
    version_no     int4           default 1,
    status         char           default '0'::bpchar,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    del_flag       char           default '0'::bpchar,
    remark         varchar(500)   default null::varchar,
    constraint pk_afo_llm_quota_account primary key (account_id)
);

comment on table afo_llm_quota_account is 'LLM 配额账户表';
comment on column afo_llm_quota_account.account_id is '配额账户ID';
comment on column afo_llm_quota_account.tenant_id is '系统租户编号';
comment on column afo_llm_quota_account.project_id is '项目ID';
comment on column afo_llm_quota_account.client_id is '应用客户端ID';
comment on column afo_llm_quota_account.object_id is '额度对象ID：项目ID/应用ID/部门ID/用户ID/API Key ID';
comment on column afo_llm_quota_account.account_type is '账户类型（tenant/project/app/department/user/api_key）';
comment on column afo_llm_quota_account.quota_unit is '额度类型（amount/token）';
comment on column afo_llm_quota_account.currency is '币种';
comment on column afo_llm_quota_account.quota_amount is '配额金额';
comment on column afo_llm_quota_account.used_amount is '已用金额';
comment on column afo_llm_quota_account.balance_amount is '可用余额';
comment on column afo_llm_quota_account.frozen_amount is '冻结金额';
comment on column afo_llm_quota_account.reset_cycle is '重置周期（none/daily/weekly/monthly/yearly）';
comment on column afo_llm_quota_account.reset_time is '下次重置时间';
comment on column afo_llm_quota_account.previous_account_id is '上一个周期配额账户ID';
comment on column afo_llm_quota_account.active_flag is '是否当前有效（0否 1是）';
comment on column afo_llm_quota_account.reset_clock is '重置触发时间点';
comment on column afo_llm_quota_account.reset_day is '重置日期：weekly 为 1-7，monthly 为 1-28，daily 为空';
comment on column afo_llm_quota_account.effective_time is '额度生效时间';
comment on column afo_llm_quota_account.limit_action is '额度耗尽后的执行动作（continue/block）';
comment on column afo_llm_quota_account.warning_enabled is '是否启用预警（0否 1是）';
comment on column afo_llm_quota_account.warning_threshold is '预警阈值百分比';
comment on column afo_llm_quota_account.version_no is '乐观锁版本号';
comment on column afo_llm_quota_account.status is '状态（0正常 1冻结 2耗尽 3已作废）';
comment on column afo_llm_quota_account.create_dept is '创建部门';
comment on column afo_llm_quota_account.create_by is '创建者';
comment on column afo_llm_quota_account.create_time is '创建时间';
comment on column afo_llm_quota_account.update_by is '更新者';
comment on column afo_llm_quota_account.update_time is '更新时间';
comment on column afo_llm_quota_account.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_quota_account.remark is '备注';

create index if not exists idx_afo_llm_quota_account_scope on afo_llm_quota_account (tenant_id, project_id, client_id, account_type, status, active_flag, del_flag);
comment on index idx_afo_llm_quota_account_scope is '查询场景：兼容旧调用按租户、项目、应用定位配额账户';
create index if not exists idx_afo_llm_quota_account_object on afo_llm_quota_account (tenant_id, account_type, object_id, quota_unit, currency, status, active_flag, del_flag);
comment on index idx_afo_llm_quota_account_object is '查询场景：按额度对象、额度类型和币种定位配额账户';
create unique index if not exists uk_afo_llm_quota_account_scope on afo_llm_quota_account (
    tenant_id,
    account_type,
    coalesce(object_id, '-1'::bigint),
    quota_unit,
    currency
) where del_flag = '0' and active_flag = '1';
comment on index uk_afo_llm_quota_account_scope is '唯一约束：未删除数据中同一租户、额度对象、额度类型和币种只能有一个配额账户，避免额度校验命中多账户';
create index if not exists idx_afo_llm_quota_account_reset on afo_llm_quota_account (active_flag, status, reset_time);
comment on index idx_afo_llm_quota_account_reset is '查询场景：定时任务筛选需要重置的配额账户';

-- ----------------------------
-- 9、LLM 配额流水
-- ----------------------------
create table if not exists afo_llm_quota_ledger
(
    ledger_id         int8,
    tenant_id         varchar(20)    default '000000'::varchar,
    account_id        int8           not null,
    request_id        varchar(64)    default null::varchar,
    biz_type          varchar(30)    not null,
    change_type       varchar(30)    not null,
    change_amount     numeric(20, 6) not null,
    balance_after     numeric(20, 6) not null,
    currency          varchar(10)    default 'USD'::varchar,
    related_record_id int8           default null,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    del_flag          char           default '0'::bpchar,
    remark            varchar(500)   default null::varchar,
    constraint pk_afo_llm_quota_ledger primary key (ledger_id)
);

comment on table afo_llm_quota_ledger is 'LLM 配额流水表';
comment on column afo_llm_quota_ledger.ledger_id is '流水ID';
comment on column afo_llm_quota_ledger.tenant_id is '系统租户编号';
comment on column afo_llm_quota_ledger.account_id is '配额账户ID';
comment on column afo_llm_quota_ledger.request_id is '请求ID';
comment on column afo_llm_quota_ledger.biz_type is '业务类型（recharge/consume/refund/adjust/reset）';
comment on column afo_llm_quota_ledger.change_type is '变动方向（increase/decrease）';
comment on column afo_llm_quota_ledger.change_amount is '变动金额';
comment on column afo_llm_quota_ledger.balance_after is '变动后余额';
comment on column afo_llm_quota_ledger.currency is '币种';
comment on column afo_llm_quota_ledger.related_record_id is '关联业务记录ID';
comment on column afo_llm_quota_ledger.create_dept is '创建部门';
comment on column afo_llm_quota_ledger.create_by is '创建者';
comment on column afo_llm_quota_ledger.create_time is '创建时间';
comment on column afo_llm_quota_ledger.update_by is '更新者';
comment on column afo_llm_quota_ledger.update_time is '更新时间';
comment on column afo_llm_quota_ledger.remark is '备注';

create index if not exists idx_afo_llm_quota_ledger_account_time on afo_llm_quota_ledger (tenant_id, account_id, create_time);
comment on index idx_afo_llm_quota_ledger_account_time is '查询场景：按账户和时间查询额度流水明细';
create index if not exists idx_afo_llm_quota_ledger_request on afo_llm_quota_ledger (request_id);
comment on index idx_afo_llm_quota_ledger_request is '查询场景：按 requestId 追踪单次调用的额度扣减流水';

-- ----------------------------
-- 10、LLM 额度守卫事件
-- ----------------------------
create table if not exists afo_llm_quota_guard_event
(
    event_id             int8,
    tenant_id            varchar(20)    default '000000'::varchar,
    request_id           varchar(100)   default null::varchar,
    model_code           varchar(100)   default null::varchar,
    event_type           varchar(40)    not null,
    decision_result      varchar(20)    default null::varchar,
    gateway_action       varchar(20)    default null::varchar,
    deny_reason          varchar(100)   default null::varchar,
    http_status          int4           default null,
    prompt_tokens        int4           default null,
    completion_tokens    int4           default null,
    total_tokens         int4           default null,
    quota_account_id     int8           default null,
    quota_unit           varchar(20)    default null::varchar,
    quota_amount         numeric(20, 6) default null,
    quota_used_amount    numeric(20, 6) default null,
    quota_balance_amount numeric(20, 6) default null,
    quota_threshold      numeric(5, 2)  default null,
    limit_action         varchar(20)    default null::varchar,
    notice_id            int8           default null,
    decision_latency_ms  int8           default null,
    event_time           timestamp      default null,
    create_dept          int8,
    create_by            int8,
    create_time          timestamp,
    update_by            int8,
    update_time          timestamp,
    constraint pk_afo_llm_quota_guard_event primary key (event_id)
);

comment on table afo_llm_quota_guard_event is 'LLM 额度守卫事件';
comment on column afo_llm_quota_guard_event.event_id is '事件ID';
comment on column afo_llm_quota_guard_event.tenant_id is '租户 ID';
comment on column afo_llm_quota_guard_event.request_id is '请求 ID';
comment on column afo_llm_quota_guard_event.model_code is '模型编码';
comment on column afo_llm_quota_guard_event.event_type is '事件类型：QUOTA_WARNING 额度预警，QUOTA_CONTINUE 超限继续使用，QUOTA_BLOCK 拦截熔断';
comment on column afo_llm_quota_guard_event.decision_result is '判定结果：ALLOWED 允许，DENIED 拒绝';
comment on column afo_llm_quota_guard_event.gateway_action is '网关实际动作：FORWARDED 放行，BLOCKED 拦截';
comment on column afo_llm_quota_guard_event.deny_reason is '拒绝原因';
comment on column afo_llm_quota_guard_event.http_status is 'HTTP 状态码';
comment on column afo_llm_quota_guard_event.prompt_tokens is '模型返回实际输入 token';
comment on column afo_llm_quota_guard_event.completion_tokens is '模型返回实际输出 token';
comment on column afo_llm_quota_guard_event.total_tokens is '模型返回实际总 token';
comment on column afo_llm_quota_guard_event.quota_account_id is '额度账户ID';
comment on column afo_llm_quota_guard_event.quota_unit is '额度类型（amount/token）';
comment on column afo_llm_quota_guard_event.quota_amount is '总额度';
comment on column afo_llm_quota_guard_event.quota_used_amount is '已用额度';
comment on column afo_llm_quota_guard_event.quota_balance_amount is '剩余额度';
comment on column afo_llm_quota_guard_event.quota_threshold is '预警阈值百分比';
comment on column afo_llm_quota_guard_event.limit_action is '超限动作（continue/block）';
comment on column afo_llm_quota_guard_event.notice_id is '预警通知公告ID';
comment on column afo_llm_quota_guard_event.decision_latency_ms is '额度守卫判定耗时毫秒';
comment on column afo_llm_quota_guard_event.event_time is '事件发生时间';

create index if not exists idx_afo_llm_quota_guard_event_tenant_time on afo_llm_quota_guard_event (tenant_id, event_time desc);
comment on index idx_afo_llm_quota_guard_event_tenant_time is '查询场景：按租户和事件时间查询额度事件';
create index if not exists idx_afo_llm_quota_guard_event_type on afo_llm_quota_guard_event (tenant_id, event_type, event_time);
comment on index idx_afo_llm_quota_guard_event_type is '查询场景：按租户、事件类型和时间查询额度事件';
create index if not exists idx_afo_llm_quota_guard_event_account on afo_llm_quota_guard_event (tenant_id, quota_account_id, event_time);
comment on index idx_afo_llm_quota_guard_event_account is '查询场景：按额度账户和时间查询额度事件';
create index if not exists idx_afo_llm_quota_guard_event_request on afo_llm_quota_guard_event (request_id);
comment on index idx_afo_llm_quota_guard_event_request is '查询场景：按 requestId 追踪单次调用的额度事件';

-- ----------------------------
-- 11、LLM 请求日志
-- ----------------------------
create table if not exists afo_llm_request_log
(
    request_log_id int8,
    tenant_id      varchar(20)   default '000000'::varchar,
    request_id     varchar(64)   not null,
    project_id     int8          default null,
    client_id      int8          default null,
    key_id         int8          default null,
    model_code     varchar(100)  default null::varchar,
    request_path   varchar(200)  default null::varchar,
    stream         char          default '0'::bpchar,
    request_status varchar(30)   default 'pending'::varchar,
    http_status    int4          default null,
    latency_ms     int8          default null,
    error_code     varchar(100)  default null::varchar,
    error_message  varchar(1000) default null::varchar,
    client_ip      varchar(128)  default null::varchar,
    user_agent     varchar(500)  default null::varchar,
    trace_id       varchar(100)  default null::varchar,
    usage_id       int8          default null,
    usage_raw      text          default '{}'::text,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    remark         varchar(500)  default null::varchar,
    constraint pk_afo_llm_request_log primary key (request_log_id)
);

comment on table afo_llm_request_log is 'LLM 请求日志表';
comment on column afo_llm_request_log.request_log_id is '请求日志ID';
comment on column afo_llm_request_log.tenant_id is '系统租户编号';
comment on column afo_llm_request_log.request_id is '请求ID';
comment on column afo_llm_request_log.project_id is '项目ID';
comment on column afo_llm_request_log.client_id is '应用客户端ID';
comment on column afo_llm_request_log.key_id is 'API Key ID';
comment on column afo_llm_request_log.model_code is '模型编码';
comment on column afo_llm_request_log.request_path is '请求路径';
comment on column afo_llm_request_log.stream is '是否流式请求（0否 1是）';
comment on column afo_llm_request_log.request_status is '请求状态';
comment on column afo_llm_request_log.http_status is 'HTTP 状态码';
comment on column afo_llm_request_log.latency_ms is '请求耗时毫秒';
comment on column afo_llm_request_log.error_code is '错误编码';
comment on column afo_llm_request_log.error_message is '错误信息';
comment on column afo_llm_request_log.client_ip is '客户端 IP';
comment on column afo_llm_request_log.user_agent is 'User-Agent';
comment on column afo_llm_request_log.trace_id is '链路追踪ID';
comment on column afo_llm_request_log.usage_id is '用量记录ID';
comment on column afo_llm_request_log.usage_raw is '模型返回的原始 usage JSON 文本';
comment on column afo_llm_request_log.create_dept is '创建部门';
comment on column afo_llm_request_log.create_by is '创建者';
comment on column afo_llm_request_log.create_time is '创建时间';
comment on column afo_llm_request_log.update_by is '更新者';
comment on column afo_llm_request_log.update_time is '更新时间';
comment on column afo_llm_request_log.remark is '备注';

drop index if exists uk_afo_llm_request_log_request;
create unique index if not exists uk_afo_llm_request_log_tenant_request on afo_llm_request_log (tenant_id, request_id);
comment on index uk_afo_llm_request_log_tenant_request is '唯一约束：同一租户下 requestId 唯一，便于链路追踪和幂等记账';
create index if not exists idx_afo_llm_request_log_client_time on afo_llm_request_log (tenant_id, client_id, create_time);
comment on index idx_afo_llm_request_log_client_time is '查询场景：按应用和时间分页查询调用日志';
create index if not exists idx_afo_llm_request_log_status_time on afo_llm_request_log (tenant_id, request_status, create_time);
comment on index idx_afo_llm_request_log_status_time is '查询场景：按请求状态和时间统计成功率、失败率';

-- ----------------------------
-- 11、LLM 用量记录
-- ----------------------------
create table if not exists afo_llm_usage_record
(
    usage_id          int8,
    tenant_id         varchar(20)    default '000000'::varchar,
    request_id        varchar(64)    not null,
    project_id        int8           default null,
    client_id         int8           default null,
    model_code        varchar(100)   not null,
    provider          varchar(50)    default null::varchar,
    prompt_tokens     int8           default 0,
    completion_tokens int8           default 0,
    total_tokens      int8           default 0,
    cached_tokens     int8           default 0,
    reasoning_tokens  int8           default 0,
    usage_raw         text           default '{}'::text,
    pricing_mode      varchar(20)    default 'token'::varchar,
    billing_quantity  numeric(20, 8) default 0,
    request_count     int8           default 0,
    duration_seconds  numeric(20, 3) default 0,
    cost_amount       numeric(20, 6) default 0,
    currency          varchar(10)    default 'USD'::varchar,
    price_id          int8           default null,
    account_id        int8           default null,
    prompt_amount     numeric(20, 6) default 0,
    completion_amount numeric(20, 6) default 0,
    total_amount      numeric(20, 6) default 0,
    billing_status    varchar(30)    default 'billed'::varchar,
    billing_time      timestamp      default null,
    usage_time        timestamp      not null,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    remark            varchar(500)   default null::varchar,
    constraint pk_afo_llm_usage_record primary key (usage_id)
);

comment on table afo_llm_usage_record is 'LLM 用量记录表';
comment on column afo_llm_usage_record.usage_id is '用量记录ID';
comment on column afo_llm_usage_record.tenant_id is '系统租户编号';
comment on column afo_llm_usage_record.request_id is '请求ID';
comment on column afo_llm_usage_record.project_id is '项目ID';
comment on column afo_llm_usage_record.client_id is '应用客户端ID';
comment on column afo_llm_usage_record.model_code is '模型编码';
comment on column afo_llm_usage_record.provider is '模型厂商';
comment on column afo_llm_usage_record.prompt_tokens is '输入 token 数';
comment on column afo_llm_usage_record.completion_tokens is '输出 token 数';
comment on column afo_llm_usage_record.total_tokens is '总 token 数';
comment on column afo_llm_usage_record.cached_tokens is '缓存命中 token 数';
comment on column afo_llm_usage_record.reasoning_tokens is '推理输出 token 数';
comment on column afo_llm_usage_record.usage_raw is 'LiteLLM 原始 usage JSON 文本';
comment on column afo_llm_usage_record.pricing_mode is '计费模式（token/request/second）';
comment on column afo_llm_usage_record.billing_quantity is '本次计费数量';
comment on column afo_llm_usage_record.request_count is '请求次数';
comment on column afo_llm_usage_record.duration_seconds is '调用耗时秒数';
comment on column afo_llm_usage_record.cost_amount is '本次调用成本金额';
comment on column afo_llm_usage_record.currency is '币种';
comment on column afo_llm_usage_record.price_id is '价格ID';
comment on column afo_llm_usage_record.account_id is '配额账户ID';
comment on column afo_llm_usage_record.prompt_amount is '输入 token 计费金额';
comment on column afo_llm_usage_record.completion_amount is '输出 token 计费金额';
comment on column afo_llm_usage_record.total_amount is '总计费金额';
comment on column afo_llm_usage_record.billing_status is '计费状态';
comment on column afo_llm_usage_record.billing_time is '计费时间';
comment on column afo_llm_usage_record.usage_time is '用量发生时间';
comment on column afo_llm_usage_record.create_dept is '创建部门';
comment on column afo_llm_usage_record.create_by is '创建者';
comment on column afo_llm_usage_record.create_time is '创建时间';
comment on column afo_llm_usage_record.update_by is '更新者';
comment on column afo_llm_usage_record.update_time is '更新时间';
comment on column afo_llm_usage_record.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_usage_record.remark is '备注';

alter table afo_llm_usage_record
    add column if not exists pricing_mode varchar(20) default 'token'::varchar,
    add column if not exists billing_quantity numeric(20, 8) default 0,
    add column if not exists request_count int8 default 0,
    add column if not exists duration_seconds numeric(20, 3) default 0,
    add column if not exists account_id int8 default null,
    add column if not exists prompt_amount numeric(20, 6) default 0,
    add column if not exists completion_amount numeric(20, 6) default 0,
    add column if not exists total_amount numeric(20, 6) default 0,
    add column if not exists billing_status varchar(30) default 'billed'::varchar,
    add column if not exists billing_time timestamp default null,
    add column if not exists del_flag char default '0'::bpchar;

update afo_llm_usage_record
set pricing_mode = 'token'
where pricing_mode is null;

drop index if exists uk_afo_llm_usage_record_request;
create unique index if not exists uk_afo_llm_usage_record_tenant_request on afo_llm_usage_record (tenant_id, request_id);
comment on index uk_afo_llm_usage_record_tenant_request is '唯一约束：同一租户下一次模型请求只生成一条用量记录';
create index if not exists idx_afo_llm_usage_record_client_time on afo_llm_usage_record (tenant_id, client_id, usage_time);
comment on index idx_afo_llm_usage_record_client_time is '查询场景：按应用和时间查询 token 用量明细';
create index if not exists idx_afo_llm_usage_record_model_time on afo_llm_usage_record (tenant_id, model_code, usage_time);
comment on index idx_afo_llm_usage_record_model_time is '查询场景：按模型和时间统计 token 用量';
create index if not exists idx_afo_llm_usage_record_model_detail on afo_llm_usage_record (tenant_id, model_code) where del_flag = '0';
comment on index idx_afo_llm_usage_record_model_detail is '查询场景：模型详情按模型聚合用量';
create index if not exists idx_afo_llm_usage_record_billing_status on afo_llm_usage_record (tenant_id, billing_status, billing_time);
comment on index idx_afo_llm_usage_record_billing_status is '查询场景：按计费状态和计费时间筛选调用明细';

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'afo_llm_usage_record'
          and column_name = 'usage_raw'
          and udt_name = 'jsonb'
    ) then
        alter table afo_llm_usage_record alter column usage_raw drop default;
        alter table afo_llm_usage_record alter column usage_raw type text using usage_raw::text;
        alter table afo_llm_usage_record alter column usage_raw set default '{}'::text;
    end if;
end $$;

-- ----------------------------
-- 12、LLM 业务审计事件
-- ----------------------------
create table if not exists afo_llm_audit_event
(
    event_id      int8,
    tenant_id     varchar(20)  default '000000'::varchar,
    event_type    varchar(50)  not null,
    event_level   varchar(20)  default 'info'::varchar,
    event_source  varchar(50)  default 'spring_boot'::varchar,
    request_id    varchar(64)  default null::varchar,
    project_id    int8         default null,
    client_id     int8         default null,
    actor_user_id int8         default null,
    event_detail  text         default '{}'::text,
    event_time    timestamp    not null,
    create_dept   int8,
    create_by     int8,
    create_time   timestamp,
    update_by     int8,
    update_time   timestamp,
    remark        varchar(500) default null::varchar,
    constraint pk_afo_llm_audit_event primary key (event_id)
);

comment on table afo_llm_audit_event is 'LLM 业务审计事件表';
comment on column afo_llm_audit_event.event_id is '审计事件ID';
comment on column afo_llm_audit_event.tenant_id is '系统租户编号';
comment on column afo_llm_audit_event.event_type is '事件类型';
comment on column afo_llm_audit_event.event_level is '事件级别（info/warn/error）';
comment on column afo_llm_audit_event.event_source is '事件来源';
comment on column afo_llm_audit_event.request_id is '请求ID';
comment on column afo_llm_audit_event.project_id is '项目ID';
comment on column afo_llm_audit_event.client_id is '应用客户端ID';
comment on column afo_llm_audit_event.actor_user_id is '操作用户ID';
comment on column afo_llm_audit_event.event_detail is '事件详情 JSON 文本';
comment on column afo_llm_audit_event.event_time is '事件发生时间';
comment on column afo_llm_audit_event.create_dept is '创建部门';
comment on column afo_llm_audit_event.create_by is '创建者';
comment on column afo_llm_audit_event.create_time is '创建时间';
comment on column afo_llm_audit_event.update_by is '更新者';
comment on column afo_llm_audit_event.update_time is '更新时间';
comment on column afo_llm_audit_event.remark is '备注';

create index if not exists idx_afo_llm_audit_event_time on afo_llm_audit_event (tenant_id, event_time);
comment on index idx_afo_llm_audit_event_time is '查询场景：按租户和时间查询审计事件';
create index if not exists idx_afo_llm_audit_event_type_level on afo_llm_audit_event (tenant_id, event_type, event_level, event_time);
comment on index idx_afo_llm_audit_event_type_level is '查询场景：按事件类型和级别统计审计风险';
create index if not exists idx_afo_llm_audit_event_request on afo_llm_audit_event (request_id);
comment on index idx_afo_llm_audit_event_request is '查询场景：按 requestId 追踪单次模型调用的审计事件';

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'afo_llm_audit_event'
          and column_name = 'event_detail'
          and udt_name = 'jsonb'
    ) then
        alter table afo_llm_audit_event alter column event_detail drop default;
        alter table afo_llm_audit_event alter column event_detail type text using event_detail::text;
        alter table afo_llm_audit_event alter column event_detail set default '{}'::text;
    end if;
end $$;

-- ----------------------------
-- 15、初始化字典类型
-- ----------------------------
delete from sys_dict_data where tenant_id = '000000' and dict_type = 'llm_price_status';
delete from sys_dict_type where tenant_id = '000000' and dict_type = 'llm_price_status';

insert into sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (2000, '000000', 'LLM模型类型', 'llm_model_type', 103, 1, now(), null, null, 'LLM模型类型列表'),
    (2001, '000000', 'LLM Key状态', 'llm_key_status', 103, 1, now(), null, null, 'LLM API Key状态列表'),
    (2002, '000000', 'LLM调用来源', 'llm_call_source', 103, 1, now(), null, null, 'LLM调用来源列表'),
    (2003, '000000', 'LLM计费单位', 'llm_billing_unit', 103, 1, now(), null, null, 'LLM计费单位列表'),
    (2004, '000000', 'LLM账单状态', 'llm_billing_status', 103, 1, now(), null, null, 'LLM账单状态列表'),
    (2005, '000000', 'LLM请求状态', 'llm_request_status', 103, 1, now(), null, null, 'LLM请求状态列表'),
    (2007, '000000', 'LLM额度状态', 'llm_quota_status', 103, 1, now(), null, null, 'LLM额度状态列表'),
    (2008, '000000', 'LLM应用类型', 'llm_app_type', 103, 1, now(), null, null, 'LLM应用类型列表'),
    (2009, '000000', 'LLM额度账户类型', 'llm_quota_account_type', 103, 1, now(), null, null, 'LLM额度账户类型列表'),
    (2010, '000000', 'LLM额度重置周期', 'llm_quota_reset_cycle', 103, 1, now(), null, null, 'LLM额度重置周期列表'),
    (2011, '000000', 'LLM额度业务类型', 'llm_quota_biz_type', 103, 1, now(), null, null, 'LLM额度业务类型列表'),
    (2012, '000000', 'LLM额度变动方向', 'llm_quota_change_type', 103, 1, now(), null, null, 'LLM额度变动方向列表'),
    (2013, '000000', 'LLM账单汇总状态', 'llm_summary_status', 103, 1, now(), null, null, 'LLM账单汇总状态列表'),
    (2014, '000000', 'LLM审计事件级别', 'llm_audit_level', 103, 1, now(), null, null, 'LLM审计事件级别列表')
on conflict (tenant_id, dict_type) do nothing;

-- ----------------------------
-- 16、初始化字典数据
-- ----------------------------
insert into sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (20000, '000000', 1, 'dict.llm_model_type.chat', 'chat', 'llm_model_type', '', 'primary', 'Y', 103, 1, now(), null, null, 'Chat Completions 模型'),
    (20001, '000000', 2, 'dict.llm_model_type.embedding', 'embedding', 'llm_model_type', '', 'info', 'N', 103, 1, now(), null, null, 'Embedding 模型'),
    (20002, '000000', 3, 'dict.llm_model_type.image', 'image', 'llm_model_type', '', 'warning', 'N', 103, 1, now(), null, null, '图片生成或理解模型'),
    (20003, '000000', 4, 'dict.llm_model_type.audio', 'audio', 'llm_model_type', '', 'warning', 'N', 103, 1, now(), null, null, '音频模型'),
    (20010, '000000', 1, 'dict.llm_key_status.normal', '0', 'llm_key_status', '', 'primary', 'Y', 103, 1, now(), null, null, 'Key正常'),
    (20011, '000000', 2, 'dict.llm_key_status.disabled', '1', 'llm_key_status', '', 'error', 'N', 103, 1, now(), null, null, 'Key停用'),
    (20020, '000000', 1, 'dict.llm_call_source.api', 'api', 'llm_call_source', '', 'primary', 'Y', 103, 1, now(), null, null, '外部API调用'),
    (20021, '000000', 2, 'dict.llm_call_source.console', 'console', 'llm_call_source', '', 'info', 'N', 103, 1, now(), null, null, '管理控制台调用'),
    (20022, '000000', 3, 'dict.llm_call_source.internal', 'internal', 'llm_call_source', '', 'info', 'N', 103, 1, now(), null, null, '内部服务调用'),
    (20030, '000000', 1, 'dict.llm_billing_unit.1k_tokens', '1k_tokens', 'llm_billing_unit', '', 'primary', 'Y', 103, 1, now(), null, null, '按千 token 计费'),
    (20031, '000000', 2, 'dict.llm_billing_unit.1m_tokens', '1m_tokens', 'llm_billing_unit', '', 'info', 'N', 103, 1, now(), null, null, '按百万 token 计费'),
    (20032, '000000', 3, 'dict.llm_billing_unit.request', 'request', 'llm_billing_unit', '', 'warning', 'N', 103, 1, now(), null, null, '按请求次数计费'),
    (20040, '000000', 1, 'dict.llm_billing_status.pending', 'pending', 'llm_billing_status', '', 'warning', 'Y', 103, 1, now(), null, null, '待计费'),
    (20041, '000000', 2, 'dict.llm_billing_status.billed', 'billed', 'llm_billing_status', '', 'primary', 'N', 103, 1, now(), null, null, '已完成计费'),
    (20042, '000000', 3, 'dict.llm_billing_status.failed', 'failed', 'llm_billing_status', '', 'error', 'N', 103, 1, now(), null, null, '计费失败'),
    (20043, '000000', 4, 'dict.llm_billing_status.reversed', 'reversed', 'llm_billing_status', '', 'info', 'N', 103, 1, now(), null, null, '已冲正'),
    (20050, '000000', 1, 'dict.llm_request_status.pending', 'pending', 'llm_request_status', '', 'warning', 'Y', 103, 1, now(), null, null, '请求待处理'),
    (20051, '000000', 2, 'dict.llm_request_status.success', 'success', 'llm_request_status', '', 'primary', 'N', 103, 1, now(), null, null, '请求成功'),
    (20052, '000000', 3, 'dict.llm_request_status.failed', 'failed', 'llm_request_status', '', 'error', 'N', 103, 1, now(), null, null, '请求失败'),
    (20053, '000000', 4, 'dict.llm_request_status.timeout', 'timeout', 'llm_request_status', '', 'warning', 'N', 103, 1, now(), null, null, '请求超时'),
    (20070, '000000', 1, 'dict.llm_quota_status.normal', '0', 'llm_quota_status', '', 'primary', 'Y', 103, 1, now(), null, null, '额度账户正常'),
    (20071, '000000', 2, 'dict.llm_quota_status.frozen', '1', 'llm_quota_status', '', 'warning', 'N', 103, 1, now(), null, null, '额度账户冻结'),
    (20072, '000000', 3, 'dict.llm_quota_status.exhausted', '2', 'llm_quota_status', '', 'error', 'N', 103, 1, now(), null, null, '额度已耗尽'),
    (20073, '000000', 4, 'dict.llm_quota_status.voided', '3', 'llm_quota_status', '', 'info', 'N', 103, 1, now(), null, null, '额度账户已作废'),
    (20080, '000000', 1, 'dict.llm_app_type.server', 'server', 'llm_app_type', '', 'primary', 'Y', 103, 1, now(), null, null, '服务端应用'),
    (20081, '000000', 2, 'dict.llm_app_type.web', 'web', 'llm_app_type', '', 'info', 'N', 103, 1, now(), null, null, 'Web应用'),
    (20082, '000000', 3, 'dict.llm_app_type.mobile', 'mobile', 'llm_app_type', '', 'warning', 'N', 103, 1, now(), null, null, '移动端应用'),
    (20083, '000000', 4, 'dict.llm_app_type.internal', 'internal', 'llm_app_type', '', 'info', 'N', 103, 1, now(), null, null, '内部应用'),
    (20090, '000000', 1, 'dict.llm_quota_account_type.tenant', 'tenant', 'llm_quota_account_type', '', 'primary', 'Y', 103, 1, now(), null, null, '租户级额度账户'),
    (20091, '000000', 2, 'dict.llm_quota_account_type.project', 'project', 'llm_quota_account_type', '', 'info', 'N', 103, 1, now(), null, null, '项目级额度账户'),
    (20092, '000000', 3, 'dict.llm_quota_account_type.app', 'app', 'llm_quota_account_type', '', 'warning', 'N', 103, 1, now(), null, null, '应用级额度账户'),
    (20093, '000000', 4, 'dict.llm_quota_account_type.department', 'department', 'llm_quota_account_type', '', 'info', 'N', 103, 1, now(), null, null, '部门级额度账户'),
    (20094, '000000', 5, 'dict.llm_quota_account_type.user', 'user', 'llm_quota_account_type', '', 'info', 'N', 103, 1, now(), null, null, '人员级额度账户'),
    (20095, '000000', 6, 'dict.llm_quota_account_type.api_key', 'api_key', 'llm_quota_account_type', '', 'warning', 'N', 103, 1, now(), null, null, 'API Key 级额度账户'),
    (20100, '000000', 1, 'dict.llm_quota_reset_cycle.none', 'none', 'llm_quota_reset_cycle', '', 'info', 'Y', 103, 1, now(), null, null, '不自动重置'),
    (20101, '000000', 2, 'dict.llm_quota_reset_cycle.daily', 'daily', 'llm_quota_reset_cycle', '', 'primary', 'N', 103, 1, now(), null, null, '每日重置'),
    (20102, '000000', 3, 'dict.llm_quota_reset_cycle.monthly', 'monthly', 'llm_quota_reset_cycle', '', 'success', 'N', 103, 1, now(), null, null, '每月重置'),
    (20103, '000000', 4, 'dict.llm_quota_reset_cycle.yearly', 'yearly', 'llm_quota_reset_cycle', '', 'warning', 'N', 103, 1, now(), null, null, '每年重置'),
    (20104, '000000', 5, 'dict.llm_quota_reset_cycle.weekly', 'weekly', 'llm_quota_reset_cycle', '', 'info', 'N', 103, 1, now(), null, null, '每周重置'),
    (20110, '000000', 1, 'dict.llm_quota_biz_type.recharge', 'recharge', 'llm_quota_biz_type', '', 'success', 'Y', 103, 1, now(), null, null, '额度充值'),
    (20111, '000000', 2, 'dict.llm_quota_biz_type.consume', 'consume', 'llm_quota_biz_type', '', 'warning', 'N', 103, 1, now(), null, null, '模型调用消费'),
    (20112, '000000', 3, 'dict.llm_quota_biz_type.refund', 'refund', 'llm_quota_biz_type', '', 'info', 'N', 103, 1, now(), null, null, '额度退款'),
    (20113, '000000', 4, 'dict.llm_quota_biz_type.adjust', 'adjust', 'llm_quota_biz_type', '', 'primary', 'N', 103, 1, now(), null, null, '人工调整'),
    (20114, '000000', 5, 'dict.llm_quota_biz_type.reset', 'reset', 'llm_quota_biz_type', '', 'info', 'N', 103, 1, now(), null, null, '周期重置'),
    (20120, '000000', 1, 'dict.llm_quota_change_type.increase', 'increase', 'llm_quota_change_type', '', 'success', 'Y', 103, 1, now(), null, null, '增加额度'),
    (20121, '000000', 2, 'dict.llm_quota_change_type.decrease', 'decrease', 'llm_quota_change_type', '', 'warning', 'N', 103, 1, now(), null, null, '减少额度'),
    (20130, '000000', 1, 'dict.llm_summary_status.generated', 'generated', 'llm_summary_status', '', 'primary', 'Y', 103, 1, now(), null, null, '账单汇总已生成'),
    (20131, '000000', 2, 'dict.llm_summary_status.confirmed', 'confirmed', 'llm_summary_status', '', 'success', 'N', 103, 1, now(), null, null, '账单汇总已确认'),
    (20132, '000000', 3, 'dict.llm_summary_status.reversed', 'reversed', 'llm_summary_status', '', 'warning', 'N', 103, 1, now(), null, null, '账单汇总已冲正'),
    (20140, '000000', 1, 'dict.llm_audit_level.info', 'info', 'llm_audit_level', '', 'info', 'Y', 103, 1, now(), null, null, '信息级审计事件'),
    (20141, '000000', 2, 'dict.llm_audit_level.warn', 'warn', 'llm_audit_level', '', 'warning', 'N', 103, 1, now(), null, null, '警告级审计事件'),
    (20142, '000000', 3, 'dict.llm_audit_level.error', 'error', 'llm_audit_level', '', 'error', 'N', 103, 1, now(), null, null, '错误级审计事件')
on conflict (dict_code) do nothing;

-- ----------------------------
-- 17、初始化系统参数
-- ----------------------------
insert into sys_config (config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (2000, '000000', 'LiteLLM基础地址', 'llm.gateway.baseUrl', 'http://127.0.0.1:4000', 'Y', 103, 1, now(), null, null, 'LiteLLM Proxy基础地址'),
    (2001, '000000', 'LiteLLM网关密钥', 'llm.gateway.apiKey', '', 'Y', 103, 1, now(), null, null, 'LiteLLM Proxy访问密钥，生产环境通过安全配置覆盖'),
    (2002, '000000', 'LiteLLM默认超时时间', 'llm.gateway.timeoutMillis', '60000', 'Y', 103, 1, now(), null, null, '调用LiteLLM的默认超时时间，单位毫秒'),
    (2003, '000000', 'LiteLLM默认重试次数', 'llm.gateway.retryTimes', '1', 'Y', 103, 1, now(), null, null, '调用LiteLLM失败后的默认重试次数'),
    (2004, '000000', 'LLM是否开启流式输出', 'llm.gateway.streamEnabled', 'true', 'Y', 103, 1, now(), null, null, '是否允许业务接口透传SSE流式输出'),
    (2005, '000000', 'LLM调用日志脱敏开关', 'llm.audit.maskEnabled', 'true', 'Y', 103, 1, now(), null, null, '是否对调用日志中的敏感内容脱敏'),
    (2006, '000000', 'LLM默认展示币种', 'llm.billing.defaultCurrency', 'USD', 'Y', 103, 1, now(), null, null, 'LLM账单和价格默认展示币种'),
    (2007, '000000', 'LiteLLM网关启用开关', 'llm.gateway.enabled', 'true', 'Y', 103, 1, now(), null, null, '是否启用LiteLLM Proxy调用'),
    (2008, '000000', 'LiteLLM Chat Completions路径', 'llm.gateway.chatCompletionsPath', '/v1/chat/completions', 'Y', 103, 1, now(), null, null, 'LiteLLM OpenAI兼容聊天补全路径')
on conflict (config_id) do nothing;

-- ----------------------------
-- 18、初始化菜单与权限
-- ----------------------------
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (18000, 'route.llm', 0, 6, 'llm', 'Layout', '', 1, 0, 'M', '0', '0', '', 'carbon:machine-learning-model', 103, 1, now(), null, null, 'LLM模型平台目录'),
    (18001, 'route.llm_model', 18000, 1, 'model', 'llm/model/index', '', 1, 0, 'C', '0', '0', 'llm:model:list', 'carbon:model', 103, 1, now(), null, null, 'LLM模型管理菜单'),
    (18002, 'route.llm_project', 18000, 2, 'project', 'llm/project/index', '', 1, 0, 'C', '0', '0', 'llm:project:list', 'carbon:folder-details', 103, 1, now(), null, null, 'LLM项目管理菜单'),
    (18003, 'route.llm_app-client', 18000, 3, 'app-client', 'llm/app-client/index', '', 1, 0, 'C', '0', '0', 'llm:appClient:list', 'carbon:application', 103, 1, now(), null, null, 'LLM应用客户端管理菜单'),
    (18004, 'route.llm_api-key', 18000, 4, 'api-key', 'llm/api-key/index', '', 1, 0, 'C', '0', '0', 'llm:apiKey:list', 'carbon:api-1', 103, 1, now(), null, null, 'LLM API Key管理菜单'),
    (18005, 'route.llm_model-price', 18000, 5, 'model-price', 'llm/model-price/index', '', 1, 0, 'C', '0', '0', 'llm:modelPrice:list', 'carbon:currency-dollar', 103, 1, now(), null, null, 'LLM企业模型价格管理菜单'),
    (18009, 'route.llm_request-log', 18000, 9, 'request-log', 'llm/request-log/index', '', 1, 0, 'C', '0', '0', 'llm:requestLog:list', 'carbon:request-quote', 103, 1, now(), null, null, 'LLM请求日志管理菜单'),
    (18014, 'route.llm_provider', 18000, 14, 'provider', 'llm/provider/index', '', 1, 0, 'C', '0', '0', 'llm:provider:list', 'carbon:logo-digg', 103, 1, now(), null, null, 'LLM厂商管理菜单'),
    (18015, 'route.llm_public-model', 18000, 15, 'public-model', 'llm/public-model/index', '', 1, 0, 'C', '0', '0', 'llm:publicModel:list', 'carbon:model-alt', 103, 1, now(), null, null, 'LLM公共模型库管理菜单')
on conflict (menu_id) do nothing;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (18101, '模型查询', 18001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:model:query', '#', 103, 1, now(), null, null, ''),
    (18102, '模型新增', 18001, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:model:add', '#', 103, 1, now(), null, null, ''),
    (18103, '模型修改', 18001, 3, '#', '', '', 1, 0, 'F', '0', '0', 'llm:model:edit', '#', 103, 1, now(), null, null, ''),
    (18104, '模型删除', 18001, 4, '#', '', '', 1, 0, 'F', '0', '0', 'llm:model:remove', '#', 103, 1, now(), null, null, ''),
    (18105, '模型导出', 18001, 5, '#', '', '', 1, 0, 'F', '0', '0', 'llm:model:export', '#', 103, 1, now(), null, null, ''),
    (18106, '网关调用', 18001, 6, '#', '', '', 1, 0, 'F', '0', '0', 'llm:chat:invoke', '#', 103, 1, now(), null, null, ''),
    (18111, '项目查询', 18002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:project:query', '#', 103, 1, now(), null, null, ''),
    (18112, '项目新增', 18002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:project:add', '#', 103, 1, now(), null, null, ''),
    (18113, '项目修改', 18002, 3, '#', '', '', 1, 0, 'F', '0', '0', 'llm:project:edit', '#', 103, 1, now(), null, null, ''),
    (18114, '项目删除', 18002, 4, '#', '', '', 1, 0, 'F', '0', '0', 'llm:project:remove', '#', 103, 1, now(), null, null, ''),
    (18115, '项目导出', 18002, 5, '#', '', '', 1, 0, 'F', '0', '0', 'llm:project:export', '#', 103, 1, now(), null, null, ''),
    (18121, '应用查询', 18003, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:appClient:query', '#', 103, 1, now(), null, null, ''),
    (18122, '应用新增', 18003, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:appClient:add', '#', 103, 1, now(), null, null, ''),
    (18123, '应用修改', 18003, 3, '#', '', '', 1, 0, 'F', '0', '0', 'llm:appClient:edit', '#', 103, 1, now(), null, null, ''),
    (18124, '应用删除', 18003, 4, '#', '', '', 1, 0, 'F', '0', '0', 'llm:appClient:remove', '#', 103, 1, now(), null, null, ''),
    (18125, '应用导出', 18003, 5, '#', '', '', 1, 0, 'F', '0', '0', 'llm:appClient:export', '#', 103, 1, now(), null, null, ''),
    (18131, 'Key查询', 18004, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:apiKey:query', '#', 103, 1, now(), null, null, ''),
    (18132, 'Key新增', 18004, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:apiKey:add', '#', 103, 1, now(), null, null, ''),
    (18133, 'Key修改', 18004, 3, '#', '', '', 1, 0, 'F', '0', '0', 'llm:apiKey:edit', '#', 103, 1, now(), null, null, ''),
    (18134, 'Key删除', 18004, 4, '#', '', '', 1, 0, 'F', '0', '0', 'llm:apiKey:remove', '#', 103, 1, now(), null, null, ''),
    (18135, 'Key导出', 18004, 5, '#', '', '', 1, 0, 'F', '0', '0', 'llm:apiKey:export', '#', 103, 1, now(), null, null, ''),
    (18141, '价格查询', 18005, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:modelPrice:query', '#', 103, 1, now(), null, null, ''),
    (18142, '价格新增', 18005, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:modelPrice:add', '#', 103, 1, now(), null, null, ''),
    (18143, '价格修改', 18005, 3, '#', '', '', 1, 0, 'F', '0', '0', 'llm:modelPrice:edit', '#', 103, 1, now(), null, null, ''),
    (18144, '价格删除', 18005, 4, '#', '', '', 1, 0, 'F', '0', '0', 'llm:modelPrice:remove', '#', 103, 1, now(), null, null, ''),
    (18145, '价格导出', 18005, 5, '#', '', '', 1, 0, 'F', '0', '0', 'llm:modelPrice:export', '#', 103, 1, now(), null, null, ''),
    (18181, '请求日志查询', 18009, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:requestLog:query', '#', 103, 1, now(), null, null, ''),
    (18182, '请求日志导出', 18009, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:requestLog:export', '#', 103, 1, now(), null, null, ''),
    (18231, '厂商查询', 18014, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:provider:query', '#', 103, 1, now(), null, null, ''),
    (18232, '厂商新增', 18014, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:provider:add', '#', 103, 1, now(), null, null, ''),
    (18233, '厂商修改', 18014, 3, '#', '', '', 1, 0, 'F', '0', '0', 'llm:provider:edit', '#', 103, 1, now(), null, null, ''),
    (18234, '厂商删除', 18014, 4, '#', '', '', 1, 0, 'F', '0', '0', 'llm:provider:remove', '#', 103, 1, now(), null, null, ''),
    (18241, '公共模型查询', 18015, 1, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModel:query', '#', 103, 1, now(), null, null, ''),
    (18242, '公共模型新增', 18015, 2, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModel:add', '#', 103, 1, now(), null, null, ''),
    (18243, '公共模型修改', 18015, 3, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModel:edit', '#', 103, 1, now(), null, null, ''),
    (18244, '公共模型删除', 18015, 4, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModel:remove', '#', 103, 1, now(), null, null, ''),
    (18245, '公共模型导入', 18015, 5, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModel:import', '#', 103, 1, now(), null, null, ''),
    (18251, '公共价格列表', 18015, 6, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModelPrice:list', '#', 103, 1, now(), null, null, ''),
    (18252, '公共价格新增', 18015, 7, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModelPrice:add', '#', 103, 1, now(), null, null, ''),
    (18253, '公共价格修改', 18015, 8, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModelPrice:edit', '#', 103, 1, now(), null, null, ''),
    (18254, '公共价格删除', 18015, 9, '#', '', '', 1, 0, 'F', '0', '0', 'llm:publicModelPrice:remove', '#', 103, 1, now(), null, null, '')
on conflict (menu_id) do nothing;

-- 授予演示角色 LLM 菜单权限，超级管理员仍按系统逻辑拥有全部权限。
insert into sys_role_menu (role_id, menu_id)
select 3, menu_id
from sys_menu
where menu_id between 18000 and 18299
on conflict (role_id, menu_id) do nothing;

-- ----------------------------
-- 19、LLM 公共模型模板
-- ----------------------------
create table if not exists afo_llm_provider
(
    provider_id       int8,
    provider_name     varchar(100) not null,
    logo_slug         varchar(100) default null::varchar,
    model_prefixes    varchar(500) default null::varchar,
    status            char         default '0'::bpchar,
    sort_order        int4         default 0,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    del_flag          char         default '0'::bpchar,
    remark            varchar(500) default null::varchar,
    constraint pk_afo_llm_provider primary key (provider_id)
);

create index if not exists idx_afo_llm_provider_status_sort on afo_llm_provider (status, sort_order, del_flag);

create table if not exists afo_llm_public_model_catalog
(
    public_model_id    int8,
    provider_id        int8         not null,
    model_code         varchar(100) not null,
    provider_model_id  varchar(150) not null,
    model_type         varchar(30)  default 'chat'::varchar,
    status             char         default '0'::bpchar,
    sort_order         int4         default 0,
    create_dept        int8,
    create_by          int8,
    create_time        timestamp,
    update_by          int8,
    update_time        timestamp,
    del_flag           char         default '0'::bpchar,
    remark             varchar(500) default null::varchar,
    constraint pk_afo_llm_public_model_catalog primary key (public_model_id)
);

create unique index if not exists uk_afo_llm_public_model_code on afo_llm_public_model_catalog (provider_id, model_code) where del_flag = '0';
create unique index if not exists uk_afo_llm_public_model_provider_model on afo_llm_public_model_catalog (provider_id, provider_model_id) where del_flag = '0';
create index if not exists idx_afo_llm_public_model_options on afo_llm_public_model_catalog (provider_id, status, sort_order, del_flag);

create table if not exists afo_llm_public_model_price
(
    public_price_id        int8,
    public_model_id        int8         not null,
    provider_id            int8         not null,
    pricing_mode           varchar(20)  default 'token'::varchar check (pricing_mode in ('tiered', 'token', 'request', 'second')),
    currency               varchar(10)  default 'USD'::varchar check (currency in ('USD', 'CNY')),
    billing_unit           varchar(30)  default '1k_tokens'::varchar,
    prompt_price           numeric(20, 8) default 0,
    completion_price       numeric(20, 8) default 0,
    request_price          numeric(20, 8) default 0,
    second_price           numeric(20, 8) default 0,
    price_source           varchar(50)  default null::varchar,
    source_url             varchar(500) default null::varchar,
    source_updated_at      timestamp,
    create_dept            int8,
    create_by              int8,
    create_time            timestamp,
    update_by              int8,
    update_time            timestamp,
    del_flag               char         default '0'::bpchar,
    remark                 varchar(500) default null::varchar,
    constraint pk_afo_llm_public_model_price primary key (public_price_id)
);

create index if not exists idx_afo_llm_public_price_lookup on afo_llm_public_model_price (public_model_id, currency, del_flag);
create index if not exists idx_afo_llm_public_price_provider on afo_llm_public_model_price (provider_id, pricing_mode, currency, del_flag);

create table if not exists afo_llm_public_model_price_tier
(
    tier_id                int8,
    public_price_id        int8 not null,
    tier_no                int4 default 1,
    min_volume             int8 default 0,
    max_volume             int8,
    usage_basis            varchar(30) default 'prompt_tokens'::varchar,
    prompt_price           numeric(20, 8) default 0,
    completion_price       numeric(20, 8) default 0,
    request_price          numeric(20, 8) default 0,
    second_price           numeric(20, 8) default 0,
    create_dept            int8,
    create_by              int8,
    create_time            timestamp,
    update_by              int8,
    update_time            timestamp,
    remark                 varchar(500) default null::varchar,
    constraint pk_afo_llm_public_model_price_tier primary key (tier_id)
);

comment on column afo_llm_public_model_price_tier.usage_basis is '阶梯匹配用量口径：total_tokens 总用量，prompt_tokens 输入用量，completion_tokens 输出用量';

alter table afo_llm_public_model_price_tier
    add column if not exists usage_basis varchar(30) default 'prompt_tokens'::varchar,
    add column if not exists request_price numeric(20, 8) default 0,
    add column if not exists second_price numeric(20, 8) default 0;

update afo_llm_public_model_price_tier
set usage_basis = 'prompt_tokens'
where usage_basis is null;

create unique index if not exists uk_afo_llm_public_price_tier_no on afo_llm_public_model_price_tier (public_price_id, tier_no);
create index if not exists idx_afo_llm_public_price_tier_volume on afo_llm_public_model_price_tier (public_price_id, min_volume, max_volume);

-- ----------------------------
-- 20、最小回滚策略
-- ----------------------------
-- 回滚菜单、参数、字典初始化数据：
-- delete from sys_role_menu where menu_id between 18000 and 18299;
-- delete from sys_menu where menu_id between 18000 and 18299;
-- delete from sys_config where config_id between 2000 and 2008;
-- delete from sys_dict_data where dict_code between 20000 and 20199;
-- delete from sys_dict_type where dict_id between 2000 and 2014;
--
-- 回滚业务表时请先确认无生产数据，再按依赖顺序执行：
-- drop table if exists afo_llm_audit_event;
-- drop table if exists afo_llm_billing_summary;
-- drop table if exists afo_llm_billing_record;
-- drop table if exists afo_llm_usage_record;
-- drop table if exists afo_llm_request_log;
-- drop table if exists afo_llm_quota_ledger;
-- drop table if exists afo_llm_quota_account;
-- drop table if exists afo_llm_customer_model_price;
-- drop table if exists afo_llm_public_model_price_tier;
-- drop table if exists afo_llm_public_model_price;
-- drop table if exists afo_llm_public_model_catalog;
-- drop table if exists afo_llm_provider;
-- drop table if exists afo_llm_model_catalog;
-- drop table if exists afo_llm_api_key;
-- drop table if exists afo_llm_app_client;
-- drop table if exists afo_llm_project;
-- drop table if exists afo_llm_tenant;
