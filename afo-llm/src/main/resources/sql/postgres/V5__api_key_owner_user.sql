alter table if exists afo_llm_api_key
    add column if not exists owner_user_id int8;

update afo_llm_api_key
set owner_user_id = coalesce(create_by, 1)
where owner_user_id is null;

alter table if exists afo_llm_api_key
    alter column owner_user_id set not null;

comment on column afo_llm_api_key.owner_user_id is '所属用户ID';
comment on column afo_llm_api_key.expire_time is '过期时间，空值表示永不过期';

alter table if exists afo_llm_api_key
    drop column if exists team_tag;

create index if not exists idx_afo_llm_api_key_owner_status
    on afo_llm_api_key (tenant_id, owner_user_id, status, del_flag);
comment on index idx_afo_llm_api_key_owner_status is '查询场景：按所属用户和状态分页查询 API Key';
