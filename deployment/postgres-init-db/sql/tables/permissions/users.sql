create table permissions.users(
  username text not null primary key,
  default_role text not null references permissions.user_roles
    on update cascade
    on delete restrict
);

comment on table permissions.users is e''
'All users recognized by this deployment.';
comment on column permissions.users.username is e''
'The user''s username. A unique identifier for this user.';
comment on column permissions.users.default_role is e''
'The user''s default role for making Hasura requests.';
