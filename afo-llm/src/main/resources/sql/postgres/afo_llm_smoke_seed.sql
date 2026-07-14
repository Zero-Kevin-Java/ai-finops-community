-- ----------------------------
-- AI-FinOps LiteLLM smoke seed data
-- 数据库: PostgreSQL
-- 用途:
-- 1. 仅用于本地或测试环境验证 /llm/v1/chat/completions 最小链路。
-- 2. 先执行 afo_llm_init.sql，再执行本脚本。
-- 3. 明文测试 Key: sk_smoke_dev_only_000000
-- 4. 默认模型映射: model_code = gpt-4o-mini, litellm_model = fake-openai-endpoint。
--    该 LiteLLM 模型别名匹配 /Users/wangbo/codex/litellm/dev_config.yaml。
--    如果本地 LiteLLM Proxy 使用其他模型名，只修改 afo_llm_model_catalog.litellm_model。
-- ----------------------------

insert into afo_llm_tenant (
    id, tenant_id, tenant_code, tenant_name, contact_name, billing_currency,
    quota_enabled, status, create_by, create_time, del_flag, remark
) select
    209900000001, '000000', 'smoke-tenant', 'LLM Smoke Tenant', 'Smoke',
    'USD', '1', '0', 1, now(), '0', 'Local smoke seed'
where not exists (
    select 1
    from afo_llm_tenant
    where tenant_id = '000000'
      and del_flag = '0'
);

insert into afo_llm_project (
    project_id, tenant_id, project_code, project_name, owner_user_id,
    status, create_by, create_time, del_flag, remark
) values (
    209900000101, '000000', 'smoke-project', 'LLM Smoke Project', 1,
    '0', 1, now(), '0', 'Local smoke seed'
) on conflict (project_id) do update set
    project_code = excluded.project_code,
    project_name = excluded.project_name,
    status = excluded.status,
    update_by = 1,
    update_time = now(),
    del_flag = '0';

insert into afo_llm_app_client (
    client_id, tenant_id, project_id, app_code, app_name, app_type,
    status, create_by, create_time, del_flag, remark
) values (
    209900000201, '000000', 209900000101, 'smoke-client', 'LLM Smoke Client', 'server',
    '0', 1, now(), '0', 'Local smoke seed'
) on conflict (client_id) do update set
    project_id = excluded.project_id,
    app_code = excluded.app_code,
    app_name = excluded.app_name,
    app_type = excluded.app_type,
    status = excluded.status,
    update_by = 1,
    update_time = now(),
    del_flag = '0';

insert into afo_llm_api_key (
    key_id, tenant_id, client_id, owner_user_id, key_name, key_prefix, key_hash,
    key_scope, expire_time, status, create_by, create_time, del_flag, remark
) values (
    209900000301, '000000', 209900000201, 1, 'LLM Smoke API Key',
    'sk_smoke_dev_only_00000', '56374690894b1694d748d5608956aeb0cdfd33ef02821afe8e494954eb919bf1',
    'chat.completions', null, '0', 1, now(), '0',
    'Local smoke seed. Plain key: sk_smoke_dev_only_000000'
) on conflict (key_id) do update set
    client_id = excluded.client_id,
    owner_user_id = excluded.owner_user_id,
    key_name = excluded.key_name,
    key_prefix = excluded.key_prefix,
    key_hash = excluded.key_hash,
    key_scope = excluded.key_scope,
    expire_time = excluded.expire_time,
    status = excluded.status,
    update_by = 1,
    update_time = now(),
    del_flag = '0';

update sys_config
set config_value = 'sk-1234',
    update_by = 1,
    update_time = now()
where config_key = 'llm.gateway.apiKey'
  and coalesce(config_value, '') = '';

insert into afo_llm_model_catalog (
    model_id, tenant_id, model_code, display_name, provider, litellm_model,
    model_type, context_window, supports_stream, supports_tool, status,
    sync_status, create_by, create_time, del_flag, remark
) values (
    209900000401, '000000', 'gpt-4o-mini', 'GPT-4o Mini Smoke', 'openai', 'fake-openai-endpoint',
    'chat', 128000, '0', '0', '0', 'synced', 1, now(), '0',
    'Local smoke seed. litellm_model matches /Users/wangbo/codex/litellm/dev_config.yaml.'
) on conflict (model_id) do update set
    model_code = excluded.model_code,
    display_name = excluded.display_name,
    provider = excluded.provider,
    litellm_model = excluded.litellm_model,
    model_type = excluded.model_type,
    sync_status = excluded.sync_status,
    status = excluded.status,
    update_by = 1,
    update_time = now(),
    del_flag = '0';

insert into afo_llm_customer_model_price (
    price_id, tenant_id, model_code, currency, billing_unit, pricing_mode,
    prompt_price, completion_price, request_price, second_price, create_by, create_time,
    del_flag, remark
) values (
    209900000601, '000000', 'gpt-4o-mini', 'USD', '1k_tokens', 'token',
    0.00100000, 0.00200000, 0, 0, 1, now(), '0', 'Local smoke seed'
) on conflict (price_id) do update set
    model_code = excluded.model_code,
    currency = excluded.currency,
    billing_unit = excluded.billing_unit,
    pricing_mode = excluded.pricing_mode,
    prompt_price = excluded.prompt_price,
    completion_price = excluded.completion_price,
    request_price = excluded.request_price,
    second_price = excluded.second_price,
    update_by = 1,
    update_time = now(),
    del_flag = '0';

insert into afo_llm_quota_account (
    account_id, tenant_id, project_id, client_id, account_type, currency,
    quota_amount, used_amount, balance_amount, frozen_amount, reset_cycle,
    reset_time, version_no, status, create_by, create_time, del_flag, remark
) values (
    209900000701, '000000', 209900000101, 209900000201, 'app', 'USD',
    100.000000, 0.000000, 100.000000, 0.000000, 'none',
    null, 1, '0', 1, now(), '0', 'Local smoke seed'
) on conflict (account_id) do update set
    project_id = excluded.project_id,
    client_id = excluded.client_id,
    account_type = excluded.account_type,
    currency = excluded.currency,
    quota_amount = excluded.quota_amount,
    balance_amount = greatest(afo_llm_quota_account.balance_amount, excluded.balance_amount),
    frozen_amount = excluded.frozen_amount,
    reset_cycle = excluded.reset_cycle,
    status = excluded.status,
    update_by = 1,
    update_time = now(),
    del_flag = '0';

-- ----------------------------
-- Rollback for smoke seed only
-- ----------------------------
-- delete from afo_llm_quota_ledger where account_id = 209900000701;
-- delete from afo_llm_usage_record where request_id like 'smoke-%';
-- delete from afo_llm_request_log where request_id like 'smoke-%';
-- delete from afo_llm_quota_account where account_id = 209900000701;
-- delete from afo_llm_customer_model_price where price_id = 209900000601;
-- delete from afo_llm_model_catalog where model_id = 209900000401;
-- delete from afo_llm_api_key where key_id = 209900000301;
-- delete from afo_llm_app_client where client_id = 209900000201;
-- delete from afo_llm_project where project_id = 209900000101;
-- delete from afo_llm_tenant where id = 209900000001;
