-- ENUMS
create type metadata.permission
  as enum ('NO_CHECK', 'OWNER', 'MISSION_MODEL_OWNER', 'PLAN_OWNER', 'PLAN_COLLABORATOR', 'PLAN_OWNER_COLLABORATOR',
    'PLAN_OWNER_SOURCE', 'PLAN_COLLABORATOR_SOURCE', 'PLAN_OWNER_COLLABORATOR_SOURCE',
    'PLAN_OWNER_TARGET', 'PLAN_COLLABORATOR_TARGET', 'PLAN_OWNER_COLLABORATOR_TARGET');

create type metadata.action_permission_key
  as enum ('simulate', 'schedule', 'insert_command_dict', 'insert_ext_dataset', 'extend_ext_dataset',
    'check_constraints', 'create_expansion_set', 'create_expansion_rule', 'expand_all_activities',
    'resource_samples', 'sequence_seq_json', 'sequence_seq_json_bulk', 'user_sequence_seq_json',
    'user_sequence_seq_json_bulk', 'get_command_dict_ts');

create type metadata.function_permission_key
  as enum ('apply_preset', 'branch_plan', 'create_merge_rq', 'withdraw_merge_rq', 'begin_merge', 'cancel_merge',
    'commit_merge', 'deny_merge', 'get_conflicting_activities', 'get_non_conflicting_activities', 'set_resolution',
    'set_resolution_bulk', 'delete_activity_subtree', 'delete_activity_subtree_bulk', 'delete_activity_reanchor_plan',
    'delete_activity_reanchor_plan_bulk', 'delete_activity_reanchor', 'delete_activity_reanchor_bulk', 'get_plan_history');

-- TABLES
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

-- NEW FUNCTIONS
create function metadata.get_role(hasura_session json)
returns text
stable
language plpgsql as $$
declare
  _role text;
  _username text;
begin
  _role := hasura_session ->> 'x-hasura-role';
  if _role is not null then
    return _role;
  end if;
  _username := hasura_session ->> 'x-hasura-user-id';
  select default_role from metadata.users u
  where u.username = _username into _role;
  if _role is null then
    raise exception 'Invalid username: %', _username;
  end if;
  return _role;
end
$$;

create function metadata.get_function_permissions(_function metadata.function_permission_key, hasura_session json)
  returns metadata.permission
  stable
  language plpgsql as $$
  declare
    _role text;
    _function_permission metadata.permission;
  begin
    _role := metadata.get_role(hasura_session);
    -- The aerie_admin role is always treated as having NO_CHECK permissions on all functions.
    if _role = 'aerie_admin' then return 'NO_CHECK'; end if;

    select (function_permissions ->> _function::text)::metadata.permission
    from metadata.user_role_permission urp
    where urp.role = _role
    into _function_permission;

    -- The absence of the function key means that the role does not have permission to perform the function.
    if _function_permission is null then
      raise insufficient_privilege
        using message = 'User with role '''|| _role ||''' is not permitted to run '''|| _function ||'''';
    end if;

    return _function_permission::metadata.permission;
  end
  $$;

create procedure metadata.check_general_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id integer, _user text)
language plpgsql as $$
declare
  _mission_model_id integer;
  _plan_name text;
begin
  select name from public.plan where id = _plan_id into _plan_name;

  -- MISSION_MODEL_OWNER: The user must own the relevant Mission Model
  if _permission = 'MISSION_MODEL_OWNER' then
    select id from public.mission_model mm
    where mm.id = (select model_id from plan p where p.id = _plan_id)
    into _mission_model_id;

    if not exists(select * from public.mission_model mm where mm.id = _mission_model_id and mm.owner =_user) then
        raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not MISSION_MODEL_OWNER on Model ' || _mission_model_id ||'.';
    end if;

  -- OWNER: The user must be the owner of all relevant objects directly used by the KEY
  -- In most cases, OWNER is equivalent to PLAN_OWNER. Use a custom solution when that is not true.
  elseif _permission = 'OWNER' then
		if not exists(select * from public.plan p where p.id = _plan_id and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not OWNER on Plan ' || _plan_id ||' ('|| _plan_name ||').';
    end if;

  -- PLAN_OWNER: The user must be the Owner of the relevant Plan
  elseif _permission = 'PLAN_OWNER' then
    if not exists(select * from public.plan p where p.id = _plan_id and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER on Plan '|| _plan_id ||' ('|| _plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR:	The user must be a Collaborator of the relevant Plan. The Plan Owner is NOT considered a Collaborator of the Plan
  elseif _permission = 'PLAN_COLLABORATOR' then
    if not exists(select * from public.plan_collaborators pc where pc.plan_id = _plan_id and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_COLLABORATOR on Plan '|| _plan_id ||' ('|| _plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR:	The user must be either the Owner or a Collaborator of the relevant Plan
  elseif _permission = 'PLAN_OWNER_COLLABORATOR' then
    if not exists(select * from public.plan p where p.id = _plan_id and p.owner = _user) then
      if not exists(select * from public.plan_collaborators pc where pc.plan_id = _plan_id and pc.collaborator = _user) then
        raise insufficient_privilege
          using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER_COLLABORATOR on Plan '|| _plan_id ||' ('|| _plan_name ||').';
      end if;
    end if;
  end if;
end
$$;

create function metadata.raise_if_plan_merge_permission(_function metadata.function_permission_key, _permission metadata.permission)
returns void
immutable
language plpgsql as $$
begin
  if _permission::text = any(array['PLAN_OWNER_SOURCE', 'PLAN_COLLABORATOR_SOURCE', 'PLAN_OWNER_COLLABORATOR_SOURCE',
    'PLAN_OWNER_TARGET', 'PLAN_COLLABORATOR_TARGET', 'PLAN_OWNER_COLLABORATOR_TARGET'])
  then
    raise 'Invalid Permission: The Permission ''%'' may not be applied to function ''%''', _permission, _function;
  end if;
end
$$;

create procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _merge_request_id integer, hasura_session json)
language plpgsql as $$
declare
  _plan_id_receiving_changes integer;
  _plan_id_supplying_changes integer;
  _function_permission metadata.permission;
  _user text;
begin
  select plan_id_receiving_changes
  from merge_request mr
  where mr.id = _merge_request_id
  into _plan_id_receiving_changes;

  select plan_id
  from public.plan_snapshot ps, merge_request mr
  where mr.id = _merge_request_id and ps.snapshot_id = mr.snapshot_id_supplying_changes
  into _plan_id_supplying_changes;

  _user := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := metadata.get_function_permissions('get_non_conflicting_activities', hasura_session);
  call metadata.check_merge_permissions(_function, _function_permission, _plan_id_receiving_changes,
    _plan_id_supplying_changes, _user);
end
$$;

create procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id_receiving integer, _plan_id_supplying integer, _user text)
language plpgsql as $$
declare
  _supplying_plan_name text;
  _receiving_plan_name text;
begin
  select name from public.plan where id = _plan_id_supplying into _supplying_plan_name;
  select name from public.plan where id = _plan_id_receiving into _receiving_plan_name;

  -- MISSION_MODEL_OWNER: The user must own the relevant Mission Model
  if _permission = 'MISSION_MODEL_OWNER' then
    call metadata.check_general_permissions(_function, _permission, _plan_id_receiving, _user);

  -- OWNER: The user must be the Owner of both Plans
  elseif _permission = 'OWNER' then
    if not (exists(select * from public.plan p where p.id = _plan_id_receiving and p.owner = _user)) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not OWNER on Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||').';
    elseif not (exists(select * from public.plan p2 where p2.id = _plan_id_supplying and p2.owner = _user)) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not OWNER on Plan '|| _plan_id_supplying
                          ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_OWNER: The user must be the Owner of either Plan
  elseif _permission = 'PLAN_OWNER' then
    if not exists(select *
                  from public.plan p
                  where (p.id = _plan_id_receiving or p.id = _plan_id_supplying)
                    and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not PLAN_OWNER on either Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||') or Plan '|| _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR:	The user must be a Collaborator of either Plan. The Plan Owner is NOT considered a Collaborator of the Plan
  elseif _permission = 'PLAN_COLLABORATOR' then
    if not exists(select *
                  from public.plan_collaborators pc
                  where (pc.plan_id = _plan_id_receiving or pc.plan_id = _plan_id_supplying)
                    and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not PLAN_COLLABORATOR on either Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||') or Plan '|| _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR:	The user must be either the Owner or a Collaborator of either Plan
  elseif _permission = 'PLAN_OWNER_COLLABORATOR' then
    if not exists(select *
                  from public.plan p
                  where (p.id = _plan_id_receiving or p.id = _plan_id_supplying)
                    and p.owner = _user) then
      if not exists(select *
                    from public.plan_collaborators pc
                    where (pc.plan_id = _plan_id_receiving or pc.plan_id = _plan_id_supplying)
                      and pc.collaborator = _user) then
        raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not PLAN_OWNER_COLLABORATOR on either Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||') or Plan '|| _plan_id_supplying ||' ('|| _supplying_plan_name ||').';

      end if;
    end if;

  -- PLAN_OWNER_SOURCE:	The user must be the Owner of the Supplying Plan
  elseif _permission = 'PLAN_OWNER_SOURCE' then
    if not exists(select *
                  from public.plan p
                  where p.id = _plan_id_supplying and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER on Source Plan '
                          || _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR_SOURCE: The user must be a Collaborator of the Supplying Plan.
  elseif _permission = 'PLAN_COLLABORATOR_SOURCE' then
    if not exists(select *
                  from public.plan_collaborators pc
                  where pc.plan_id = _plan_id_supplying and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_COLLABORATOR on Source Plan '
                          || _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR_SOURCE:	The user must be either the Owner or a Collaborator of the Supplying Plan.
  elseif _permission = 'PLAN_OWNER_COLLABORATOR_SOURCE' then
    if not exists(select *
                  from public.plan p
                  where p.id = _plan_id_supplying and p.owner = _user) then
      if not exists(select *
                    from public.plan_collaborators pc
                    where pc.plan_id = _plan_id_supplying and pc.collaborator = _user) then
        raise insufficient_privilege
          using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER_COLLABORATOR on Source Plan '
                          || _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
      end if;
    end if;

  -- PLAN_OWNER_TARGET: The user must be the Owner of the Receiving Plan.
  elseif _permission = 'PLAN_OWNER_TARGET' then
    if not exists(select *
                  from public.plan p
                  where p.id = _plan_id_receiving and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER on Target Plan '
                          || _plan_id_receiving ||' ('|| _receiving_plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR_TARGET: The user must be a Collaborator of the Receiving Plan.
  elseif _permission = 'PLAN_COLLABORATOR_TARGET' then
    if not exists(select *
                  from public.plan_collaborators pc
                  where pc.plan_id = _plan_id_receiving and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_COLLABORATOR on Target Plan '
                          || _plan_id_receiving ||' ('|| _receiving_plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR_TARGET: The user must be either the Owner or a Collaborator of the Receiving Plan.
  elseif _permission = 'PLAN_OWNER_COLLABORATOR_TARGET' then
    if not exists(select *
                  from public.plan p
                  where p.id = _plan_id_receiving and p.owner = _user) then
      if not exists(select *
                    from public.plan_collaborators pc
                    where pc.plan_id = _plan_id_receiving and pc.collaborator = _user) then
        raise insufficient_privilege
          using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER_COLLABORATOR on Target Plan '
                          || _plan_id_receiving ||' ('|| _receiving_plan_name ||').';
      end if;
    end if;
  end if;
end
$$;

call migrations.mark_migration_applied('21');
