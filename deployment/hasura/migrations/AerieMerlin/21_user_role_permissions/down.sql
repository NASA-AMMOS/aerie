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
