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

-- UPDATE VOLATILITY
alter function hasura_functions.get_resources_at_start_offset(_dataset_id integer, _start_offset interval) stable;

-- PERMISSIONS IN HASURA FUNCTIONS
drop function hasura_functions.apply_preset_to_activity(_preset_id integer, _activity_id integer, _plan_id integer);
create function hasura_functions.apply_preset_to_activity(_preset_id int, _activity_id int, _plan_id int, hasura_session json)
returns activity_directive
strict
volatile
language plpgsql as $$
  declare
    returning_directive activity_directive;
    ad_activity_type text;
    preset_activity_type text;
    _function_permission metadata.permission;
    _user text;
begin
    _function_permission := metadata.get_function_permissions('apply_preset', hasura_session);
    perform metadata.raise_if_plan_merge_permission('apply_preset', _function_permission);
    -- Check valid permissions
    _user := hasura_session ->> 'x-hasura-user-id';
    if not _function_permission = 'NO_CHECK' then
      if _function_permission = 'OWNER' then
        if not exists(select * from public.activity_presets ap where ap.id = _preset_id and ap.owner = _user) then
          raise insufficient_privilege
            using message = 'Cannot run ''apply_preset'': '''|| _user ||''' is not OWNER on Activity Preset '
                            || _preset_id ||'.';
        end if;
      end if;
      -- Additionally, the user needs to be OWNER of the plan
      call metadata.check_general_permissions('apply_preset', _function_permission, _plan_id, _user);
    end if;

    if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity directive % does not exist in plan %', _activity_id, _plan_id;
    end if;
    if not exists(select id from public.activity_presets where id = _preset_id) then
      raise exception 'Activity preset % does not exist', _preset_id;
    end if;

    select type from activity_directive where (id, plan_id) = (_activity_id, _plan_id) into ad_activity_type;
    select associated_activity_type from activity_presets where id = _preset_id into preset_activity_type;

    if (ad_activity_type != preset_activity_type) then
      raise exception 'Cannot apply preset for activity type "%" onto an activity of type "%".', preset_activity_type, ad_activity_type;
    end if;

    update activity_directive
    set arguments = (select arguments from activity_presets where id = _preset_id)
    where (id, plan_id) = (_activity_id, _plan_id);

    insert into preset_to_directive(preset_id, activity_id, plan_id)
    select _preset_id, _activity_id, _plan_id
    on conflict (activity_id, plan_id) do update
    set preset_id = _preset_id;

    select * from activity_directive
    where (id, plan_id) = (_activity_id, _plan_id)
    into returning_directive;

    return returning_directive;
end
$$;

drop function hasura_functions.delete_activity_by_pk_reanchor_plan_start(_activity_id int, _plan_id int);
create function hasura_functions.delete_activity_by_pk_reanchor_plan_start(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    _function_permission metadata.permission;
  begin
    _function_permission := metadata.get_function_permissions('delete_activity_reanchor_plan', hasura_session);
    perform metadata.raise_if_plan_merge_permission('delete_activity_reanchor_plan', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call metadata.check_general_permissions('delete_activity_reanchor_plan', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
    end if;

    return query
      with updated as (
        select public.anchor_direct_descendents_to_plan(_activity_id := _activity_id, _plan_id := _plan_id)
      )
      select updated.*, 'updated'
        from updated;

    return query
      with deleted as (
        delete from activity_directive where (id, plan_id) = (_activity_id, _plan_id) returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
  end
$$;

drop function hasura_functions.delete_activity_by_pk_reanchor_to_anchor(_activity_id int, _plan_id int);
create function hasura_functions.delete_activity_by_pk_reanchor_to_anchor(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
  language plpgsql as $$
declare
    _function_permission metadata.permission;
begin
    _function_permission := metadata.get_function_permissions('delete_activity_reanchor', hasura_session);
    perform metadata.raise_if_plan_merge_permission('delete_activity_reanchor', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call metadata.check_general_permissions('delete_activity_reanchor', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
    end if;

    return query
      with updated as (
        select public.anchor_direct_descendents_to_ancestor(_activity_id := _activity_id, _plan_id := _plan_id)
      )
      select updated.*, 'updated'
        from updated;
    return query
      with deleted as (
        delete from activity_directive where (id, plan_id) = (_activity_id, _plan_id) returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

drop function hasura_functions.delete_activity_by_pk_delete_subtree(_activity_id int, _plan_id int);
create function hasura_functions.delete_activity_by_pk_delete_subtree(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
  language plpgsql as $$
declare
  _function_permission metadata.permission;
begin
  _function_permission := metadata.get_function_permissions('delete_activity_subtree', hasura_session);
  perform metadata.raise_if_plan_merge_permission('delete_activity_subtree', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('delete_activity_subtree', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
  end if;

  if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

  return query
    with recursive
      descendents(activity_id, p_id) as (
          select _activity_id, _plan_id
          from activity_directive ad
          where (ad.id, ad.plan_id) = (_activity_id, _plan_id)
        union
          select ad.id, ad.plan_id
          from activity_directive ad, descendents d
          where (ad.anchor_id, ad.plan_id) = (d.activity_id, d.p_id)
      ),
      deleted as (
          delete from activity_directive ad
            using descendents
            where (ad.plan_id, ad.id) = (_plan_id, descendents.activity_id)
            returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

drop function hasura_functions.delete_activity_by_pk_reanchor_plan_start_bulk(_activity_ids int[], _plan_id int);
create function hasura_functions.delete_activity_by_pk_reanchor_plan_start_bulk(_activity_ids int[], _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    activity_id int;
    _function_permission metadata.permission;
  begin
    _function_permission := metadata.get_function_permissions('delete_activity_reanchor_plan_bulk', hasura_session);
    perform metadata.raise_if_plan_merge_permission('delete_activity_reanchor_plan_bulk', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call metadata.check_general_permissions('delete_activity_reanchor_plan_bulk', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    set constraints public.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      -- An activity ID might've been deleted in a prior step, so validate that it exists first
      if exists(select id from public.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura_functions.delete_activity_by_pk_reanchor_plan_start(activity_id, _plan_id, hasura_session);
      end if;
    end loop;
    set constraints public.validate_anchors_update_trigger deferred;
  end
$$;

drop function hasura_functions.delete_activity_by_pk_reanchor_to_anchor_bulk(_activity_ids int[], _plan_id int);
create function hasura_functions.delete_activity_by_pk_reanchor_to_anchor_bulk(_activity_ids int[], _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    activity_id int;
    _function_permission metadata.permission;
  begin
    _function_permission := metadata.get_function_permissions('delete_activity_reanchor_bulk', hasura_session);
    perform metadata.raise_if_plan_merge_permission('delete_activity_reanchor_bulk', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call metadata.check_general_permissions('delete_activity_reanchor_bulk', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    set constraints public.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      -- An activity ID might've been deleted in a prior step, so validate that it exists first
      if exists(select id from public.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura_functions.delete_activity_by_pk_reanchor_to_anchor(activity_id, _plan_id, hasura_session);
      end if;
    end loop;
    set constraints public.validate_anchors_update_trigger deferred;
  end
$$;

drop function hasura_functions.delete_activity_by_pk_delete_subtree_bulk(_activity_ids int[], _plan_id int);
create function hasura_functions.delete_activity_by_pk_delete_subtree_bulk(_activity_ids int[], _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    activity_id int;
    _function_permission metadata.permission;
  begin
    _function_permission := metadata.get_function_permissions('delete_activity_subtree_bulk', hasura_session);
    perform metadata.raise_if_plan_merge_permission('delete_activity_subtree_bulk', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call metadata.check_general_permissions('delete_activity_subtree_bulk', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    set constraints public.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      if exists(select id from public.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura_functions.delete_activity_by_pk_delete_subtree(activity_id, _plan_id, hasura_session);
      end if;
    end loop;
    set constraints public.validate_anchors_update_trigger deferred;
  end
$$;

drop function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json);
create function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json)
  returns hasura_functions.duplicate_plan_return_value -- plan_id of the new plan
  volatile
  language plpgsql as $$
declare
  res integer;
  new_owner text;
  _function_permission metadata.permission;
begin
  new_owner := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := metadata.get_function_permissions('branch_plan', hasura_session);
  perform metadata.raise_if_plan_merge_permission('branch_plan', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('branch_plan', _function_permission, plan_id, new_owner);
  end if;

  select duplicate_plan(plan_id, new_plan_name, new_owner) into res;
  return row(res)::hasura_functions.duplicate_plan_return_value;
end;
$$;

drop function hasura_functions.get_plan_history(plan_id integer);
create function hasura_functions.get_plan_history(_plan_id integer, hasura_session json)
  returns setof hasura_functions.get_plan_history_return_value
  stable
  language plpgsql as $$
declare
  _function_permission metadata.permission;
begin
  _function_permission := metadata.get_function_permissions('get_plan_history', hasura_session);
  perform metadata.raise_if_plan_merge_permission('get_plan_history', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('get_plan_history', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
  end if;

  return query select get_plan_history($1);
end;
$$;

drop function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, hasura_session json);
create function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, hasura_session json)
  returns hasura_functions.create_merge_request_return_value -- plan_id of the new plan
  volatile
  language plpgsql as $$
declare
  res integer;
  requester_username text;
  _function_permission metadata.permission;
begin
  requester_username := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := metadata.get_function_permissions('create_merge_rq', hasura_session);
  call metadata.check_merge_permissions('create_merge_rq', _function_permission, target_plan_id, source_plan_id, requester_username);

  select create_merge_request(source_plan_id, target_plan_id, requester_username) into res;
  return row(res)::hasura_functions.create_merge_request_return_value;
end;
$$;

drop function hasura_functions.get_non_conflicting_activities(merge_request_id integer);
create function hasura_functions.get_non_conflicting_activities(_merge_request_id integer, hasura_session json)
  returns setof hasura_functions.get_non_conflicting_activities_return_value
  strict
  stable
  language plpgsql as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
begin
  call metadata.check_merge_permissions('get_non_conflicting_activities', _merge_request_id, hasura_session);

  select snapshot_id_supplying_changes, plan_id_receiving_changes
  from merge_request
  where merge_request.id = $1
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes;

  return query
    with plan_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, adt.directive_id
      from metadata.tags tags, metadata.activity_directive_tags adt
      where tags.id = adt.tag_id
        and adt.plan_id = _plan_id_receiving_changes
      group by adt.directive_id
    ),
    snapshot_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, sat.directive_id
      from metadata.tags tags, metadata.snapshot_activity_tags sat
      where tags.id = sat.tag_id
        and sat.snapshot_id = _snapshot_id_supplying_changes
      group by sat.directive_id
    )
    select
      activity_id,
      change_type,
      snap_act,
      act,
      coalesce(st.tags, '[]'),
      coalesce(pt.tags, '[]')
    from
      (select msa.activity_id, msa.change_type
       from merge_staging_area msa
       where msa.merge_request_id = $1) c
        left join plan_snapshot_activities snap_act
               on _snapshot_id_supplying_changes = snap_act.snapshot_id
              and c.activity_id = snap_act.id
        left join activity_directive act
               on _plan_id_receiving_changes = act.plan_id
              and c.activity_id = act.id
        left join plan_tags pt
               on c.activity_id = pt.directive_id
        left join snapshot_tags st
               on c.activity_id = st.directive_id;
end
$$;

drop function hasura_functions.get_conflicting_activities(merge_request_id integer);
create function hasura_functions.get_conflicting_activities(_merge_request_id integer, hasura_session json)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  stable
  language plpgsql as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
  _merge_base_snapshot_id integer;
begin
  call metadata.check_merge_permissions('get_conflicting_activities', _merge_request_id, hasura_session);

  select snapshot_id_supplying_changes, plan_id_receiving_changes, merge_base_snapshot_id
  from merge_request
  where merge_request.id = _merge_request_id
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes, _merge_base_snapshot_id;

  return query
    with plan_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, adt.directive_id
      from metadata.tags tags, metadata.activity_directive_tags adt
      where tags.id = adt.tag_id
        and _plan_id_receiving_changes = adt.plan_id
      group by adt.directive_id
    ), snapshot_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, sdt.directive_id, sdt.snapshot_id
      from metadata.tags tags, metadata.snapshot_activity_tags sdt
      where tags.id = sdt.tag_id
        and (sdt.snapshot_id = _snapshot_id_supplying_changes
         or sdt.snapshot_id = _merge_base_snapshot_id)
      group by sdt.directive_id, sdt.snapshot_id
    )
    select
      activity_id,
      change_type_supplying,
      change_type_receiving,
      case
        when c.resolution = 'supplying' then 'source'::resolution_type
        when c.resolution = 'receiving' then 'target'::resolution_type
        when c.resolution = 'none' then 'none'::resolution_type
      end,
      snap_act,
      act,
      merge_base_act,
      coalesce(st.tags, '[]'),
      coalesce(pt.tags, '[]'),
      coalesce(mbt.tags, '[]')
    from
      (select * from conflicting_activities c where c.merge_request_id = _merge_request_id) c
        left join plan_snapshot_activities merge_base_act
                  on c.activity_id = merge_base_act.id and _merge_base_snapshot_id = merge_base_act.snapshot_id
        left join plan_snapshot_activities snap_act
                  on c.activity_id = snap_act.id and _snapshot_id_supplying_changes = snap_act.snapshot_id
        left join activity_directive act
                  on _plan_id_receiving_changes = act.plan_id and c.activity_id = act.id
        left join plan_tags pt
                  on c.activity_id = pt.directive_id
        left join snapshot_tags st
                  on c.activity_id = st.directive_id and _snapshot_id_supplying_changes = st.snapshot_id
        left join snapshot_tags mbt
                  on c.activity_id = st.directive_id and _merge_base_snapshot_id = st.snapshot_id;
end;
$$;

drop function hasura_functions.begin_merge(merge_request_id integer, hasura_session json);
create function hasura_functions.begin_merge(_merge_request_id integer, hasura_session json)
  returns hasura_functions.begin_merge_return_value -- plan_id of the new plan
  strict
  volatile
  language plpgsql as $$
  declare
    non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[];
    conflicting_activities hasura_functions.get_conflicting_activities_return_value[];
    reviewer_username text;
begin
  call metadata.check_merge_permissions('begin_merge', _merge_request_id, hasura_session);

  reviewer_username := (hasura_session ->> 'x-hasura-user-id');
  call public.begin_merge($1, reviewer_username);

  non_conflicting_activities := array(select hasura_functions.get_non_conflicting_activities($1, hasura_session));
  conflicting_activities := array(select hasura_functions.get_conflicting_activities($1, hasura_session));

  return row($1, non_conflicting_activities, conflicting_activities)::hasura_functions.begin_merge_return_value;
end;
$$;

drop function hasura_functions.commit_merge(merge_request_id integer);
create function hasura_functions.commit_merge(_merge_request_id integer, hasura_session json)
  returns hasura_functions.commit_merge_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call metadata.check_merge_permissions('commit_merge', _merge_request_id, hasura_session);
  call commit_merge(_merge_request_id);
  return row(_merge_request_id)::hasura_functions.commit_merge_return_value;
end;
$$;

drop function hasura_functions.deny_merge(merge_request_id integer);
create function hasura_functions.deny_merge(merge_request_id integer, hasura_session json)
  returns hasura_functions.deny_merge_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call metadata.check_merge_permissions('deny_merge', $1, hasura_session);
  call deny_merge($1);
  return row($1)::hasura_functions.deny_merge_return_value;
end;
$$;

drop function hasura_functions.withdraw_merge_request(merge_request_id integer);
create function hasura_functions.withdraw_merge_request(_merge_request_id integer, hasura_session json)
  returns hasura_functions.withdraw_merge_request_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call metadata.check_merge_permissions('withdraw_merge_rq', _merge_request_id, hasura_session);
  call withdraw_merge_request(_merge_request_id);
  return row(_merge_request_id)::hasura_functions.withdraw_merge_request_return_value;
end;
$$;

drop function hasura_functions.cancel_merge(merge_request_id integer);
create function hasura_functions.cancel_merge(_merge_request_id integer, hasura_session json)
  returns hasura_functions.cancel_merge_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call metadata.check_merge_permissions('cancel_merge', _merge_request_id, hasura_session);
  call cancel_merge(_merge_request_id);
  return row(_merge_request_id)::hasura_functions.cancel_merge_return_value;
end;
$$;

drop function hasura_functions.set_resolution(_merge_request_id integer, _activity_id integer, _resolution resolution_type);
create function hasura_functions.set_resolution(_merge_request_id integer, _activity_id integer, _resolution resolution_type, hasura_session json)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  volatile
  language plpgsql as $$
  declare
    _conflict_resolution conflict_resolution;
  begin
    call metadata.check_merge_permissions('set_resolution', _merge_request_id, hasura_session);

    select into _conflict_resolution
      case
        when _resolution = 'source' then 'supplying'::conflict_resolution
        when _resolution = 'target' then 'receiving'::conflict_resolution
        when _resolution = 'none' then 'none'::conflict_resolution
      end;

      update conflicting_activities ca
      set resolution = _conflict_resolution
      where ca.merge_request_id = _merge_request_id and ca.activity_id = _activity_id;
    return query
    select * from hasura_functions.get_conflicting_activities(_merge_request_id, hasura_session)
      where activity_id = _activity_id
      limit 1;
  end
  $$;

drop function hasura_functions.set_resolution_bulk(_merge_request_id integer, _resolution resolution_type);
create function hasura_functions.set_resolution_bulk(_merge_request_id integer, _resolution resolution_type, hasura_session json)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  volatile
  language plpgsql as $$
declare
  _conflict_resolution conflict_resolution;
begin
  call metadata.check_merge_permissions('set_resolution_bulk', _merge_request_id, hasura_session);

  select into _conflict_resolution
    case
      when _resolution = 'source' then 'supplying'::conflict_resolution
      when _resolution = 'target' then 'receiving'::conflict_resolution
      when _resolution = 'none' then 'none'::conflict_resolution
      end;

  update conflicting_activities ca
  set resolution = _conflict_resolution
  where ca.merge_request_id = _merge_request_id;
  return query
    select * from hasura_functions.get_conflicting_activities(_merge_request_id, hasura_session);
end
$$;

call migrations.mark_migration_applied('21');
