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

-- Replace admin with aerie_admin
update metadata.user_roles
  set role = 'aerie_admin',
      description = 'The admin role for Aerie. Able to perform all interactions without restriction.'
  where role = 'admin';

-- 'aerie_admin' permissions aren't specified since 'aerie_admin' is always considered to have "NO_CHECK" permissions
insert into metadata.user_role_permission(role, action_permissions, function_permissions)
values
  ('aerie_admin', '{}', '{}'),
  ('user',
   '{
      "simulate":"PLAN_OWNER_COLLABORATOR",
      "schedule":"PLAN_OWNER_COLLABORATOR",
      "insert_command_dict": "NO_CHECK",
      "insert_ext_dataset": "PLAN_OWNER",
      "extend_ext_dataset": "PLAN_OWNER",
      "check_constraints": "PLAN_OWNER_COLLABORATOR",
      "create_expansion_set": "NO_CHECK",
      "create_expansion_rule": "NO_CHECK",
      "expand_all_activities": "NO_CHECK",
      "resource_samples": "NO_CHECK",
      "sequence_seq_json": "NO_CHECK",
      "sequence_seq_json_bulk": "NO_CHECK",
      "user_sequence_seq_json": "NO_CHECK",
      "user_sequence_seq_json_bulk": "NO_CHECK",
      "get_command_dict_ts": "NO_CHECK"
    }',
   '{
      "apply_preset": "PLAN_OWNER_COLLABORATOR",
      "branch_plan": "NO_CHECK",
      "create_merge_rq": "PLAN_OWNER_SOURCE",
      "withdraw_merge_rq": "PLAN_OWNER_SOURCE",
      "begin_merge": "PLAN_OWNER_TARGET",
      "cancel_merge": "PLAN_OWNER_TARGET",
      "commit_merge": "PLAN_OWNER_TARGET",
      "deny_merge": "PLAN_OWNER_TARGET",
      "get_conflicting_activities": "NO_CHECK",
      "get_non_conflicting_activities": "NO_CHECK",
      "set_resolution": "PLAN_OWNER_TARGET",
      "set_resolution_bulk": "PLAN_OWNER_TARGET",
      "delete_activity_subtree": "PLAN_OWNER_COLLABORATOR",
      "delete_activity_subtree_bulk": "PLAN_OWNER_COLLABORATOR",
      "delete_activity_reanchor_plan": "PLAN_OWNER_COLLABORATOR",
      "delete_activity_reanchor_plan_bulk": "PLAN_OWNER_COLLABORATOR",
      "delete_activity_reanchor": "PLAN_OWNER_COLLABORATOR",
      "delete_activity_reanchor_bulk": "PLAN_OWNER_COLLABORATOR",
      "get_plan_history": "NO_CHECK"
    }' ),
  ('viewer',
   '{
      "resource_samples": "NO_CHECK",
      "sequence_seq_json": "NO_CHECK",
      "sequence_seq_json_bulk": "NO_CHECK",
      "user_sequence_seq_json": "NO_CHECK",
      "user_sequence_seq_json_bulk": "NO_CHECK",
      "get_command_dict_ts": "NO_CHECK"
    }',
   '{
      "get_conflicting_activities": "NO_CHECK",
      "get_non_conflicting_activities": "NO_CHECK",
      "get_plan_history": "NO_CHECK"
    }');

call migrations.mark_migration_applied('21');
