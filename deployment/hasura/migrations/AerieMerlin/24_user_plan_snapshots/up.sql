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

call migrations.mark_migration_applied('24');
