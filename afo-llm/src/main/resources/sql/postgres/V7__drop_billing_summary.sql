drop table if exists afo_llm_billing_summary;

delete from sys_role_menu where menu_id in (18012, 18211, 18212);
delete from sys_menu where menu_id in (18211, 18212, 18012);
