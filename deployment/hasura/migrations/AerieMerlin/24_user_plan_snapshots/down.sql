-- Hasura function
drop function hasura_functions.restore_from_snapshot(_plan_id integer, _snapshot_id integer, hasura_session json);
drop function hasura_functions.create_snapshot(_plan_id integer, _snapshot_name text, hasura_session json);
drop table hasura_functions.create_snapshot_return_value;

-- Remove 'create_snapshot' key from all roles it is on
update metadata.user_role_permission
  set function_permissions = (function_permissions  - 'create_snapshot') - 'restore_snapshot';

-- Undo alphabetize metadata.action_permission_key
drop type metadata.action_permission_key;
create type metadata.action_permission_key
  as enum ('simulate', 'schedule', 'insert_ext_dataset', 'check_constraints', 'create_expansion_set',
    'create_expansion_rule', 'expand_all_activities', 'sequence_seq_json_bulk', 'resource_samples');

-- Update function permissions key enum
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id_receiving integer, _plan_id_supplying integer, _user text);
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _merge_request_id integer, hasura_session json);
drop function metadata.raise_if_plan_merge_permission(_function metadata.function_permission_key, _permission metadata.permission);
drop procedure metadata.check_general_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id integer, _user text);
drop function metadata.get_function_permissions(_function metadata.function_permission_key, hasura_session json);

drop type metadata.function_permission_key;
create type metadata.function_permission_key
  as enum ('apply_preset', 'branch_plan', 'create_merge_rq', 'withdraw_merge_rq', 'begin_merge', 'cancel_merge',
    'commit_merge', 'deny_merge', 'get_conflicting_activities', 'get_non_conflicting_activities', 'set_resolution',
    'set_resolution_bulk', 'delete_activity_subtree', 'delete_activity_subtree_bulk', 'delete_activity_reanchor_plan',
    'delete_activity_reanchor_plan_bulk', 'delete_activity_reanchor', 'delete_activity_reanchor_bulk', 'get_plan_history',
    'restore_activity_changelog');

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

-- Remove restore_from_snapshot
comment on procedure restore_from_snapshot(_plan_id integer, _snapshot_id integer) is null;
drop procedure restore_from_snapshot(_plan_id integer, _snapshot_id integer);

-- Remove override for create_snapshot
comment on function create_snapshot(integer, text, text) is null;
comment on function create_snapshot(integer) is e''
  'Create a snapshot of the specified plan. A snapshot consists of:'
  '  - The plan''s name, revision, duration, start time, and id'
  '  - All the activities in the plan'
  '  - The preset status of those activities'
  '  - The tags on those activities';

drop function create_snapshot(_plan_id integer, _user text, _snapshot_name text);
create or replace function create_snapshot(_plan_id integer)
  returns integer -- snapshot id inserted into the table
  language plpgsql as $$
  declare
    validate_plan_id integer;
    inserted_snapshot_id integer;
begin
  select id from plan where plan.id = _plan_id into validate_plan_id;
  if validate_plan_id is null then
    raise exception 'Plan % does not exist.', _plan_id;
  end if;

  insert into plan_snapshot(plan_id, revision, name, duration, start_time)
    select id, revision, name, duration, start_time
    from plan where id = _plan_id
    returning snapshot_id into inserted_snapshot_id;
  insert into plan_snapshot_activities(
                snapshot_id, id, name, source_scheduling_goal_id, created_at,
                last_modified_at, last_modified_by, start_offset, type,
                arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      inserted_snapshot_id,                              -- this is the snapshot id
      id, name, source_scheduling_goal_id, created_at,   -- these are the rest of the data for an activity row
      last_modified_at, last_modified_by, start_offset, type, arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from activity_directive where activity_directive.plan_id = _plan_id;
  insert into preset_to_snapshot_directive(preset_id, activity_id, snapshot_id)
    select ptd.preset_id, ptd.activity_id, inserted_snapshot_id
    from preset_to_directive ptd
    where ptd.plan_id = _plan_id;
  insert into metadata.snapshot_activity_tags(snapshot_id, directive_id, tag_id)
    select inserted_snapshot_id, directive_id, tag_id
    from metadata.activity_directive_tags adt
    where adt.plan_id = _plan_id;

  --all snapshots in plan_latest_snapshot for plan plan_id become the parent of the current snapshot
  insert into plan_snapshot_parent(snapshot_id, parent_snapshot_id)
    select inserted_snapshot_id, snapshot_id
    from plan_latest_snapshot where plan_latest_snapshot.plan_id = _plan_id;

  --remove all of those entries from plan_latest_snapshot and add this new snapshot.
  delete from plan_latest_snapshot where plan_latest_snapshot.plan_id = _plan_id;
  insert into plan_latest_snapshot(plan_id, snapshot_id) values (_plan_id, inserted_snapshot_id);

  return inserted_snapshot_id;
  end;
$$;


-- Update plan_snapshot
comment on table plan_snapshot is e''
  'A record of the metadata associated with a plan, excluding tags.';
comment on column plan_snapshot.snapshot_id is null;
comment on column plan_snapshot.plan_id is null;
comment on column plan_snapshot.revision is null;
comment on column plan_snapshot.snapshot_name is null;
comment on column plan_snapshot.taken_by is null;
comment on column plan_snapshot.taken_at is null;

alter table plan_snapshot
	drop constraint snapshot_name_unique_per_plan,
	drop column taken_at,
	drop column taken_by,
	drop column snapshot_name,
	add column name text,
	add column duration interval,
	add column start_time timestamptz;

update plan_snapshot ps
set start_time = p.start_time,
    name = p.name,
    duration = p.duration
from plan p
where ps.plan_id = p.id;

alter table plan_snapshot
	alter column name set not null,
	alter column duration set not null ,
	alter column start_time set not null;

call migrations.mark_migration_rolled_back('24');
