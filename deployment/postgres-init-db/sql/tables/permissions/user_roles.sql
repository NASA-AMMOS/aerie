-- This table is an enum-compatible table (https://hasura.io/docs/latest/schema/postgres/enums/#pg-create-enum-table)
create table permissions.user_roles(
  role text primary key,
  description text null
);

comment on table permissions.user_roles is e''
  'A list of all the allowed Hasura roles, with an optional description per role';

create function permissions.insert_permission_for_user_role()
  returns trigger
  security definer
language plpgsql as $$
  begin
    insert into permissions.user_role_permission(role)
    values (new.role);
    return new;
  end
$$;

create trigger insert_permissions_when_user_role_created
  after insert on permissions.user_roles
  for each row
  execute function permissions.insert_permission_for_user_role();
