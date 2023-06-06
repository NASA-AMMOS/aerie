create table metadata.users(
  username text not null primary key,
  default_role text not null references metadata.user_roles
    on update cascade
    on delete restrict
);
-- Insert the default roles into the table, then change the generated status to "Always"
-- This can be changed back if we need to add more default users in the future
insert into metadata.users(username, default_role)
  values ('Mission Model', 'viewer'),
         ('Aerie Legacy', 'viewer');

comment on table metadata.users is e''
'All users recognized by this deployment.';
comment on column metadata.users.username is e''
'The user''s username. A unique identifier for this user.';
comment on column metadata.users.default_role is e''
'The user''s default role for making Hasura requests.';
