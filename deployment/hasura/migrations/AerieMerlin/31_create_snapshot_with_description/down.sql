-- Drop Hasura function override
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

drop function hasura_functions.create_snapshot(integer, text, json, text);

-- Replace create_snapshot(integer, text, text, text) with create_snapshot(integer, text, text)
create or replace function create_snapshot(_plan_id integer)
	returns integer
	language plpgsql as $$
begin
	return create_snapshot(_plan_id, null, null);
end
$$;

drop function create_snapshot(integer, text, text, text);

create function create_snapshot(_plan_id integer, _snapshot_name text, _user text)
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
      snapshot_id, id, name, source_scheduling_goal_id, created_at, created_by,
      last_modified_at, last_modified_by, start_offset, type,
      arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      inserted_snapshot_id,                              -- this is the snapshot id
      id, name, source_scheduling_goal_id, created_at, created_by, -- these are the rest of the data for an activity row
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

call migrations.mark_migration_rolled_back('31');
