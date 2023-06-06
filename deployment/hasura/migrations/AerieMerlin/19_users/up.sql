-- USER ROLES
-- This table is an enum-compatible table (https://hasura.io/docs/latest/schema/postgres/enums/#pg-create-enum-table)
create table metadata.user_roles(
  role text primary key,
  description text null
);
insert into metadata.user_roles(role) values ('admin'), ('user'), ('viewer');

comment on table metadata.user_roles is e''
  'A list of all the allowed Hasura roles, with an optional description per role';

-- USERS
create table metadata.users(
  username text not null primary key,
  default_role text not null references metadata.user_roles
    on update cascade
    on delete restrict
);

comment on table metadata.users is e''
'All users recognized by this deployment.';
comment on column metadata.users.username is e''
'The user''s username. A unique identifier for this user.';
comment on column metadata.users.default_role is e''
'The user''s default role for making Hasura requests.';

-- USERS ALLOWED ROLES
create table metadata.users_allowed_roles(
  username text references metadata.users
    on update cascade
    on delete cascade,
  allowed_role text not null references metadata.user_roles
    on update cascade
    on delete cascade,

  primary key (username, allowed_role),

  constraint system_roles_have_no_allowed_roles
    check (username != 'Mission Model' and username != 'Aerie Legacy' )
);

comment on table metadata.users_allowed_roles is e''
'An association between a user and all of the roles they are allowed to use for Hasura requests';

-- USERS AND ROLES VIEW
create view metadata.users_and_roles as
(
  select
    u.username as username,
    -- Roles
    u.default_role as hasura_default_role,
    array_agg(r.allowed_role) filter (where r.allowed_role is not null) as hasura_allowed_roles
  from metadata.users u
  left join metadata.users_allowed_roles r using (username)
  group by u.username
);

comment on view metadata.users_and_roles is e''
'View a user''s information with their role information';

-- ADD SYSTEM USERS
insert into metadata.users(username, default_role)
  values ('Mission Model', 'viewer'),
         ('Aerie Legacy', 'viewer');

call migrations.mark_migration_applied('19');
