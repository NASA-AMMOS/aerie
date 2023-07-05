-- This table is an enum-compatible table (https://hasura.io/docs/latest/schema/postgres/enums/#pg-create-enum-table)
create table metadata.user_roles(
  role text primary key,
  description text null
);
insert into metadata.user_roles(role) values ('admin'), ('user'), ('viewer');

comment on table metadata.user_roles is e''
  'A list of all the allowed Hasura roles, with an optional description per role';
