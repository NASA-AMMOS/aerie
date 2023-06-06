-- USERS AND ROLES VIEW
comment on view metadata.users_and_roles is null;
drop view metadata.users_and_roles;

-- USERS ALLOWED ROLES
comment on table metadata.users_allowed_roles is null;
drop table metadata.users_allowed_roles;

-- USERS
comment on column metadata.users.default_role is null;
comment on column metadata.users.username is null;
comment on table metadata.users is null;
drop table metadata.users;

-- USER ROLES
comment on table metadata.user_roles is null;
drop table metadata.user_roles;

call migrations.mark_migration_rolled_back('19');
