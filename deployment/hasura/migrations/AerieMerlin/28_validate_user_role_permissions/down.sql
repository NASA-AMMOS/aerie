drop trigger validate_permissions_trigger
  on metadata.user_role_permission;
drop function metadata.validate_permissions_json();

call migrations.mark_migration_rolled_back('28');
