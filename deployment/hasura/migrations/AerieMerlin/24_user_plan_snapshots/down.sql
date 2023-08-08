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
