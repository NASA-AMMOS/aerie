create table metadata.users_allowed_roles(
  user_id integer references metadata.users
    on update cascade
    on delete cascade,
  allowed_role text not null references metadata.user_roles
    on update cascade
    on delete cascade,

  primary key (user_id, allowed_role),

  constraint system_roles_have_no_allowed_roles
    check (user_id >= 0) -- negative user ids are used for system roles.
);

comment on table metadata.users_allowed_roles is e''
'An association between a user and all of the roles they are allowed to use for Hasura requests';
