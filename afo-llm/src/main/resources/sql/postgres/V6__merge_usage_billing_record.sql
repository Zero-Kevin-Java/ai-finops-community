alter table if exists afo_llm_request_log
    add column if not exists usage_raw text default '{}'::text;
comment on column afo_llm_request_log.usage_raw is '模型返回的原始 usage JSON 文本';

alter table if exists afo_llm_usage_record
    add column if not exists pricing_mode varchar(20) default 'token'::varchar,
    add column if not exists billing_quantity numeric(20, 8) default 0,
    add column if not exists request_count int8 default 0,
    add column if not exists duration_seconds numeric(20, 3) default 0,
    add column if not exists account_id int8 default null,
    add column if not exists prompt_amount numeric(20, 6) default 0,
    add column if not exists completion_amount numeric(20, 6) default 0,
    add column if not exists total_amount numeric(20, 6) default 0,
    add column if not exists billing_status varchar(30) default 'billed'::varchar,
    add column if not exists billing_time timestamp default null;

comment on column afo_llm_usage_record.pricing_mode is '计费模式（token/request/second）';
comment on column afo_llm_usage_record.billing_quantity is '本次计费数量';
comment on column afo_llm_usage_record.request_count is '请求次数';
comment on column afo_llm_usage_record.duration_seconds is '调用耗时秒数';
comment on column afo_llm_usage_record.account_id is '配额账户ID';
comment on column afo_llm_usage_record.prompt_amount is '输入 token 计费金额';
comment on column afo_llm_usage_record.completion_amount is '输出 token 计费金额';
comment on column afo_llm_usage_record.total_amount is '总计费金额';
comment on column afo_llm_usage_record.billing_status is '计费状态';
comment on column afo_llm_usage_record.billing_time is '计费时间';

do $$
begin
    if to_regclass('afo_llm_billing_record') is not null then
        alter table afo_llm_billing_record
            add column if not exists account_id int8 default null,
            add column if not exists price_id int8 default null,
            add column if not exists currency varchar(10) default 'USD'::varchar,
            add column if not exists pricing_mode varchar(20) default null,
            add column if not exists billing_quantity numeric(20, 8) default null,
            add column if not exists prompt_amount numeric(20, 6) default 0,
            add column if not exists completion_amount numeric(20, 6) default 0,
            add column if not exists total_amount numeric(20, 6) default 0,
            add column if not exists billing_status varchar(30) default null,
            add column if not exists billing_time timestamp default null;

        update afo_llm_usage_record u
        set account_id = coalesce(u.account_id, b.account_id),
            price_id = coalesce(u.price_id, b.price_id),
            currency = coalesce(nullif(u.currency, ''), b.currency, 'USD'),
            pricing_mode = coalesce(b.pricing_mode, u.pricing_mode, 'token'),
            billing_quantity = coalesce(b.billing_quantity, u.billing_quantity, 0),
            prompt_amount = coalesce(b.prompt_amount, u.prompt_amount, 0),
            completion_amount = coalesce(b.completion_amount, u.completion_amount, 0),
            total_amount = coalesce(b.total_amount, u.total_amount, u.cost_amount, 0),
            cost_amount = coalesce(u.cost_amount, b.total_amount, 0),
            billing_status = coalesce(b.billing_status, u.billing_status, 'billed'),
            billing_time = coalesce(b.billing_time, u.billing_time, u.usage_time, u.create_time, now())
        from afo_llm_billing_record b
        where u.usage_id = b.usage_id;
    end if;
end $$;

update afo_llm_usage_record
set billing_status = coalesce(billing_status, 'billed'),
    billing_time = coalesce(billing_time, usage_time, create_time, now()),
    total_amount = coalesce(total_amount, cost_amount, 0),
    cost_amount = coalesce(cost_amount, total_amount, 0),
    request_count = coalesce(request_count, 1)
where billing_status is null
   or billing_time is null
   or total_amount is null
   or cost_amount is null
   or request_count is null;

update afo_llm_request_log
set usage_id = request_log_id
where usage_id is null
  and request_status = 'success'
  and http_status >= 200
  and http_status < 300;

insert into afo_llm_usage_record (
    usage_id, tenant_id, request_id, project_id, client_id, model_code,
    prompt_tokens, completion_tokens, total_tokens, cached_tokens, reasoning_tokens,
    usage_raw, pricing_mode, billing_quantity, request_count, duration_seconds,
    cost_amount, currency, prompt_amount, completion_amount, total_amount,
    billing_status, billing_time, usage_time, create_time
)
select
    l.usage_id,
    l.tenant_id,
    l.request_id,
    l.project_id,
    l.client_id,
    l.model_code,
    coalesce((usage_json ->> 'prompt_tokens')::int8, (usage_json ->> 'input_tokens')::int8, 0),
    coalesce((usage_json ->> 'completion_tokens')::int8, (usage_json ->> 'output_tokens')::int8, 0),
    coalesce((usage_json ->> 'total_tokens')::int8,
             (usage_json ->> 'prompt_tokens')::int8 + (usage_json ->> 'completion_tokens')::int8,
             (usage_json ->> 'input_tokens')::int8 + (usage_json ->> 'output_tokens')::int8,
             0),
    coalesce((usage_json -> 'prompt_tokens_details' ->> 'cached_tokens')::int8,
             (usage_json -> 'input_tokens_details' ->> 'cached_tokens')::int8,
             0),
    coalesce((usage_json -> 'completion_tokens_details' ->> 'reasoning_tokens')::int8,
             (usage_json -> 'output_tokens_details' ->> 'reasoning_tokens')::int8,
             0),
    coalesce(nullif(l.usage_raw, ''), '{}'::text),
    'token',
    0,
    1,
    round(coalesce(l.latency_ms, 0)::numeric / 1000, 3),
    0,
    'USD',
    0,
    0,
    0,
    'billed',
    coalesce(l.create_time, now()),
    coalesce(l.create_time, now()),
    coalesce(l.create_time, now())
from (
    select l.*, coalesce(nullif(l.usage_raw, '')::jsonb, '{}'::jsonb) as usage_json
    from afo_llm_request_log l
    where l.usage_id is not null
      and l.request_status = 'success'
      and l.http_status >= 200
      and l.http_status < 300
      and l.model_code is not null
) l
where not exists (
    select 1
    from afo_llm_usage_record u
    where u.tenant_id = l.tenant_id
      and u.request_id = l.request_id
);

create index if not exists idx_afo_llm_usage_record_billing_status
    on afo_llm_usage_record (tenant_id, billing_status, billing_time);
comment on index idx_afo_llm_usage_record_billing_status is '查询场景：按计费状态和计费时间筛选调用明细';

drop table if exists afo_llm_billing_record;

delete from sys_role_menu where menu_id in (18011, 18201, 18202);
delete from sys_menu where menu_id in (18201, 18202, 18011);
