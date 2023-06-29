-- NEW FUNCTIONS
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id_receiving integer, _snapshot_id_supplying integer, _user text);
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _merge_request_id integer, hasura_session json);
drop function metadata.raise_if_plan_merge_permission(_function metadata.function_permission_key, _permission metadata.permission);
drop procedure metadata.check_general_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id integer, _user text);
drop function metadata.get_function_permissions(_function metadata.function_permission_key, hasura_session json);
drop function metadata.get_role(hasura_session json);

-- TABLES
drop trigger insert_permissions_when_user_role_created
  on metadata.user_roles;
drop function metadata.insert_permission_for_user_role();

comment on column metadata.user_role_permission.function_permissions is null;
comment on column metadata.user_role_permission.action_permissions is null;
comment on column metadata.user_role_permission.role is null;
comment on table metadata.user_role_permission is null;

drop table metadata.user_role_permission;

-- ENUMS
drop type metadata.function_permission_key;
drop type metadata.action_permission_key;
drop type metadata.permission;

call migrations.mark_migration_rolled_back('21');
