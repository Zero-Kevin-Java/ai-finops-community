update afo_llm_api_key
set status = '0'
where status = '2';

delete from sys_dict_data
where dict_type = 'llm_key_status'
  and dict_value = '2';
