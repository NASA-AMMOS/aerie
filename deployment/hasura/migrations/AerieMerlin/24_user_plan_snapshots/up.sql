-- Update plan_snapshot
alter table plan_snapshot
	drop column name,
	drop column duration,
	drop column start_time,
	add column snapshot_name text,
	add column taken_by text,
	add column taken_at timestamptz not null default now(),
	add constraint snapshot_name_unique_per_plan
		unique (plan_id, snapshot_name);


comment on table plan_snapshot is e''
  'A record of the state of a plan at a given time.';
comment on column plan_snapshot.snapshot_id is e''
	'The identifier of the snapshot.';
comment on column plan_snapshot.plan_id is e''
	'The plan that this is a snapshot of.';
comment on column plan_snapshot.revision is e''
	'The revision of the plan at the time the snapshot was taken.';
comment on column plan_snapshot.snapshot_name is e''
	'A human-readable name for the snapshot.';
comment on column plan_snapshot.taken_by is e''
	'The user who took the snapshot.';
comment on column plan_snapshot.taken_at is e''
	'The time that the snapshot was taken.';

-- Create override for create_snapshot
create or replace function create_snapshot(_plan_id integer)
	returns integer
	language plpgsql as $$
begin
	return create_snapshot(_plan_id, null, null);
end
$$;

create function create_snapshot(_plan_id integer, _user text, _snapshot_name text)
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

  insert into plan_snapshot(plan_id, revision, snapshot_name, taken_by)
    select id, revision, _snapshot_name, _user
    from plan where id = _plan_id
    returning snapshot_id into inserted_snapshot_id;
  insert into plan_snapshot_activities(
      snapshot_id, id, name, source_scheduling_goal_id, created_at,
      last_modified_at, last_modified_by, start_offset, type,
      arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      inserted_snapshot_id,                              -- this is the snapshot id
      id, name, source_scheduling_goal_id, created_at,   -- these are the rest of the data for an activity row
      last_modified_at, last_modified_by, start_offset, type,
      arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
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

comment on function create_snapshot(integer) is e''
	'See comment on create_snapshot(integer, text, text)';

comment on function create_snapshot(integer, text, text) is e''
  'Create a snapshot of the specified plan. A snapshot consists of:'
  '  - The plan''s id and revision'
  '  - All the activities in the plan'
  '  - The preset status of those activities'
  '  - The tags on those activities'
	'  - When the snapshot was taken'
	'  - Optionally: who took the snapshot and a name';

-- Restore From Snapshot
create procedure restore_from_snapshot(_plan_id integer, _snapshot_id integer)
	language plpgsql as $$
	declare
		_snapshot_name text;
		_plan_name text;
	begin
		-- Input Validation
		select name from plan where id = _plan_id into _plan_name;
		if _plan_name is null then
			raise exception 'Cannot Restore: Plan with ID % does not exist.', _plan_id;
		end if;
		if not exists(select snapshot_id from plan_snapshot where snapshot_id = _snapshot_id) then
			raise exception 'Cannot Restore: Snapshot with ID % does not exist.', _snapshot_id;
		end if;
		if not exists(select snapshot_id from plan_snapshot where _snapshot_id = snapshot_id and _plan_id = plan_id ) then
			select snapshot_name from plan_snapshot where snapshot_id = _snapshot_id into _snapshot_name;
			if _snapshot_name is not null then
        raise exception 'Cannot Restore: Snapshot ''%'' (ID %) is not a snapshot of Plan ''%'' (ID %)',
          _snapshot_name, _snapshot_id, _plan_name, _plan_id;
      else
				raise exception 'Cannot Restore: Snapshot % is not a snapshot of Plan ''%'' (ID %)',
          _snapshot_id, _plan_name, _plan_id;
      end if;
    end if;

		-- Catch Plan_Locked
		call plan_locked_exception(_plan_id);

    -- Record the Union of Activities in Plan and Snapshot
    -- and note which ones have been added since the Snapshot was taken (in_snapshot = false)
    create temp table diff(
			activity_id integer,
			in_snapshot boolean not null
		);
		insert into diff(activity_id, in_snapshot)
		select id as activity_id, true
		from plan_snapshot_activities where snapshot_id = _snapshot_id;

		insert into diff (activity_id, in_snapshot)
		select activity_id, false
		from(
				select id as activity_id
				from activity_directive
				where plan_id = _plan_id
			except
				select activity_id
				from diff) a;

		-- Remove any added activities
  delete from activity_directive ad
		using diff d
		where (ad.id, ad.plan_id) = (d.activity_id, _plan_id)
			and d.in_snapshot is false;

		-- Upsert the rest
		insert into activity_directive (
		      id, plan_id, name, source_scheduling_goal_id, created_at, last_modified_at, last_modified_by,
		      start_offset, type, arguments, last_modified_arguments_at, metadata,
		      anchor_id, anchored_to_start)
		select psa.id, _plan_id, psa.name, psa.source_scheduling_goal_id, psa.created_at, psa.last_modified_at, psa.last_modified_by,
		       psa.start_offset, psa.type, psa.arguments, psa.last_modified_arguments_at, psa.metadata,
		       psa.anchor_id, psa.anchored_to_start
		from plan_snapshot_activities psa
		where psa.snapshot_id = _snapshot_id
		on conflict (id, plan_id) do update
		-- 'last_modified_at' and 'last_modified_arguments_at' are skipped during update, as triggers will overwrite them to now()
		set name = excluded.name,
		    source_scheduling_goal_id = excluded.source_scheduling_goal_id,
		    created_at = excluded.created_at,
		    last_modified_by = excluded.last_modified_by,
		    start_offset = excluded.start_offset,
		    type = excluded.type,
		    arguments = excluded.arguments,
		    metadata = excluded.metadata,
		    anchor_id = excluded.anchor_id,
		    anchored_to_start = excluded.anchored_to_start;

		-- Tags
		delete from metadata.activity_directive_tags adt
		using diff d
		where (adt.directive_id, adt.plan_id) = (d.activity_id, _plan_id);

		insert into metadata.activity_directive_tags(directive_id, plan_id, tag_id)
		select sat.directive_id, _plan_id, sat.tag_id
		from metadata.snapshot_activity_tags sat
		where sat.snapshot_id = _snapshot_id
		on conflict (directive_id, plan_id, tag_id) do nothing;

		-- Presets
		delete from preset_to_directive
		  where plan_id = _plan_id;
		insert into preset_to_directive(preset_id, activity_id, plan_id)
			select pts.preset_id, pts.activity_id, _plan_id
			from preset_to_snapshot_directive pts
			where pts.snapshot_id = _snapshot_id
			on conflict (activity_id, plan_id)
			do update	set preset_id = excluded.preset_id;

		-- Clean up
		drop table diff;
  end
$$;

comment on procedure restore_from_snapshot(_plan_id integer, _snapshot_id integer) is e''
	'Restore a plan to its state described in the given snapshot.';

-- Update function permissions key enum
-- Normally, adding to the type can be done via a command like
-- alter type metadata.function_permission_key add value 'create_snapshot' after 'create_merge_rq';
-- However, since this migration needs to fix the order of the enum, it is also being dropped and recreated in the up
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id_receiving integer, _plan_id_supplying integer, _user text);
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _merge_request_id integer, hasura_session json);
drop function metadata.raise_if_plan_merge_permission(_function metadata.function_permission_key, _permission metadata.permission);
drop procedure metadata.check_general_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id integer, _user text);
drop function metadata.get_function_permissions(_function metadata.function_permission_key, hasura_session json);

drop type metadata.function_permission_key;
create type metadata.function_permission_key
  as enum (
    'apply_preset',
    'begin_merge',
    'branch_plan',
    'cancel_merge',
    'commit_merge',
    'create_merge_rq',
    'create_snapshot',
    'delete_activity_reanchor',
    'delete_activity_reanchor_bulk',
    'delete_activity_reanchor_plan',
    'delete_activity_reanchor_plan_bulk',
    'delete_activity_subtree',
    'delete_activity_subtree_bulk',
    'deny_merge',
    'get_conflicting_activities',
    'get_non_conflicting_activities',
    'get_plan_history',
    'restore_activity_changelog',
    'restore_snapshot',
    'set_resolution',
    'set_resolution_bulk',
    'withdraw_merge_rq'
  );

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

-- Alphabetize metadata.action_permission_key
drop type metadata.action_permission_key;
create type metadata.action_permission_key
  as enum (
    'check_constraints',
    'create_expansion_rule',
    'create_expansion_set',
    'expand_all_activities',
    'insert_ext_dataset',
    'resource_samples',
    'schedule',
    'sequence_seq_json_bulk',
    'simulate'
  );

-- Add new key to user role
update metadata.user_role_permission
  set function_permissions = function_permissions
                               || jsonb_build_object('create_snapshot', 'PLAN_OWNER_COLLABORATOR')
                               || jsonb_build_object('restore_snapshot', 'PLAN_OWNER_COLLABORATOR')
where role = 'user';

-- Hasura function
create table hasura_functions.create_snapshot_return_value(snapshot_id integer);
create function hasura_functions.create_snapshot(_plan_id integer, _snapshot_name text, hasura_session json)
  returns hasura_functions.create_snapshot_return_value
  volatile
  language plpgsql as $$
declare
  _snapshot_id integer;
  _snapshotter text;
  _function_permission metadata.permission;
begin
  _snapshotter := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := metadata.get_function_permissions('create_snapshot', hasura_session);
  perform metadata.raise_if_plan_merge_permission('create_snapshot', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('create_snapshot', _function_permission, _plan_id, _snapshotter);
  end if;
  if _snapshot_name is null then
    raise exception 'Snapshot name cannot be null.';
  end if;

  select create_snapshot(_plan_id, _snapshot_name, _snapshotter) into _snapshot_id;
  return row(_snapshot_id)::hasura_functions.create_snapshot_return_value;
end;
$$;
create function hasura_functions.restore_from_snapshot(_plan_id integer, _snapshot_id integer, hasura_session json)
	returns hasura_functions.create_snapshot_return_value
	volatile
	language plpgsql as $$
declare
  _user text;
  _function_permission metadata.permission;
begin
	_user := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := metadata.get_function_permissions('restore_snapshot', hasura_session);
  perform metadata.raise_if_plan_merge_permission('restore_snapshot', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('restore_snapshot', _function_permission, _plan_id, _user);
  end if;

  call restore_from_snapshot(_plan_id, _snapshot_id);
  return row(_snapshot_id)::hasura_functions.create_snapshot_return_value;
end
$$;

call migrations.mark_migration_applied('24');
