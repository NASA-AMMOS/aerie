create table metadata.user_role_permission(
  role text not null
    primary key
    references metadata.user_roles
      on update cascade
      on delete cascade,
  action_permissions jsonb not null default '{}',
  function_permissions jsonb not null default '{}'
);

comment on table metadata.user_role_permission is e''
  'Permissions for a role that cannot be expressed in Hasura. Permissions take the form {KEY:PERMISSION}.'
  'A list of valid KEYs and PERMISSIONs can be found at https://github.com/NASA-AMMOS/aerie/discussions/983#discussioncomment-6257146';
comment on column metadata.user_role_permission.role is e''
  'The role these permissions apply to.';
comment on column metadata.user_role_permission.action_permissions is ''
  'The permissions the role has on Hasura Actions.';
comment on column metadata.user_role_permission.function_permissions is ''
  'The permissions the role has on Hasura Functions.';

