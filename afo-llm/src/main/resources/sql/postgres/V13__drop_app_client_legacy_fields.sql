alter table if exists afo_llm_app_client
    drop column if exists owner_user_id,
    drop column if exists callback_url,
    drop column if exists allowed_origins;
