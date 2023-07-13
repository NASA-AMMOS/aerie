-- This table is an enum-compatible table (https://hasura.io/docs/latest/schema/postgres/enums/#pg-create-enum-table)
create table metadata.user_roles(
  role text primary key,
  description text null
);
insert into metadata.user_roles(role) values ('aerie_admin'), ('user'), ('viewer');

comment on table metadata.user_roles is e''
  'A list of all the allowed Hasura roles, with an optional description per role';

create function metadata.insert_permission_for_user_role()
  returns trigger
  security definer
language plpgsql as $$
  begin
    insert into metadata.user_role_permission(role)
    values (new.role);
    return new;
  end
$$;

create trigger insert_permissions_when_user_role_created
  after insert on metadata.user_roles
  for each row
  execute function metadata.insert_permission_for_user_role();
