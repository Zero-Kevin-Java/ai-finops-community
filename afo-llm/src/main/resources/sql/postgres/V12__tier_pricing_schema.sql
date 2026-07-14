alter table if exists afo_llm_model_price_tier
    add column if not exists tier_no int4 default 1,
    add column if not exists usage_basis varchar(30) default 'prompt_tokens',
    add column if not exists request_price numeric(20, 8) default 0,
    add column if not exists second_price numeric(20, 8) default 0,
    add column if not exists remark varchar(500) default null::varchar;

alter table if exists afo_llm_public_model_price_tier
    add column if not exists tier_no int4 default 1,
    add column if not exists usage_basis varchar(30) default 'prompt_tokens',
    add column if not exists request_price numeric(20, 8) default 0,
    add column if not exists second_price numeric(20, 8) default 0,
    add column if not exists remark varchar(500) default null::varchar;

alter table if exists afo_llm_model_price_tier
    alter column prompt_price type numeric(20, 8),
    alter column completion_price type numeric(20, 8);

alter table if exists afo_llm_public_model_price_tier
    alter column prompt_price type numeric(20, 8),
    alter column completion_price type numeric(20, 8);

alter table if exists afo_llm_model_price_tier
    alter column usage_basis set default 'prompt_tokens';

alter table if exists afo_llm_public_model_price_tier
    alter column usage_basis set default 'prompt_tokens';

update afo_llm_model_price_tier
set usage_basis = 'prompt_tokens'
where usage_basis is null
   or usage_basis not in ('total_tokens', 'prompt_tokens', 'completion_tokens');

update afo_llm_public_model_price_tier
set usage_basis = 'prompt_tokens'
where usage_basis is null
   or usage_basis not in ('total_tokens', 'prompt_tokens', 'completion_tokens');

with ranked as (
    select tier_id,
           row_number() over (
               partition by price_id
               order by coalesce(tier_no, 2147483647), min_volume, tier_id
           ) as rn
    from afo_llm_model_price_tier
)
update afo_llm_model_price_tier t
set tier_no = ranked.rn
from ranked
where t.tier_id = ranked.tier_id
  and (t.tier_no is null or t.tier_no <> ranked.rn);

with ranked as (
    select tier_id,
           row_number() over (
               partition by public_price_id
               order by coalesce(tier_no, 2147483647), min_volume, tier_id
           ) as rn
    from afo_llm_public_model_price_tier
)
update afo_llm_public_model_price_tier t
set tier_no = ranked.rn
from ranked
where t.tier_id = ranked.tier_id
  and (t.tier_no is null or t.tier_no <> ranked.rn);

drop index if exists uk_afo_llm_model_price_tier_no;
create unique index if not exists uk_afo_llm_model_price_tier_no on afo_llm_model_price_tier (price_id, tier_no);
create index if not exists idx_afo_llm_model_price_tier_volume on afo_llm_model_price_tier (price_id, min_volume, max_volume);

drop index if exists uk_afo_llm_public_price_tier_no;
create unique index if not exists uk_afo_llm_public_price_tier_no on afo_llm_public_model_price_tier (public_price_id, tier_no);
create index if not exists idx_afo_llm_public_price_tier_volume on afo_llm_public_model_price_tier (public_price_id, min_volume, max_volume);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_afo_llm_model_price_tier_usage_basis') then
        alter table afo_llm_model_price_tier
            add constraint ck_afo_llm_model_price_tier_usage_basis
            check (usage_basis in ('total_tokens', 'prompt_tokens', 'completion_tokens'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'ck_afo_llm_public_price_tier_usage_basis') then
        alter table afo_llm_public_model_price_tier
            add constraint ck_afo_llm_public_price_tier_usage_basis
            check (usage_basis in ('total_tokens', 'prompt_tokens', 'completion_tokens'));
    end if;
end $$;

comment on column afo_llm_model_price_tier.tier_no is '阶梯序号';
comment on column afo_llm_model_price_tier.usage_basis is '阶梯匹配用量口径：total_tokens 总用量，prompt_tokens 输入用量，completion_tokens 输出用量';
comment on column afo_llm_model_price_tier.request_price is '请求单价';
comment on column afo_llm_model_price_tier.second_price is '秒级单价';
comment on column afo_llm_model_price_tier.remark is '备注';

comment on column afo_llm_public_model_price_tier.tier_no is '阶梯序号';
comment on column afo_llm_public_model_price_tier.usage_basis is '阶梯匹配用量口径：total_tokens 总用量，prompt_tokens 输入用量，completion_tokens 输出用量';
comment on column afo_llm_public_model_price_tier.request_price is '请求阶梯单价';
comment on column afo_llm_public_model_price_tier.second_price is '秒级阶梯单价';
comment on column afo_llm_public_model_price_tier.remark is '备注';
