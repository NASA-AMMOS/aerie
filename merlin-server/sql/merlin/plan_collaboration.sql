-- TODO list:
--   - plan snapshot table (done)
--   - duplicate function
--   - merge request table
--   - diff function
--   - history tracking (tables and function)
--   - merge base function

create table plan_snapshot_parent(
  snapshot_id integer,
  parent_snapshot_id integer
);

create table plan_latest_snapshot(
  plan_id integer,
  snapshot_id integer
);

-- Snapshot is a collection of the state of all the activities as they were at the time of the snapshot
-- as well as any other properties of the plan that can change
create table plan_snapshot(
  snapshot_id integer generated always as identity,

  plan_id integer not null,
  revision integer not null,
  name text not null,
  duration interval not null,
  start_time timestamptz not null
);

create table plan_snapshot_activities(
  snapshot_id integer,
  id integer,
  name text,
  tags text[],
  source_scheduling_goal_id integer,
  created_at timestamptz not null,
  last_modified_at timestamptz not null,
  start_offset interval not null,
  type text not null,
  arguments merlin_argument_set not null,
  last_modified_arguments_at timestamptz not null,
  metadata merlin_activity_directive_metadata_set
);

create or replace function create_snapshot(plan_id integer)
  returns integer -- snapshot id inserted into the table
  language plpgsql as $$
  declare
    validate_planid integer;
    inserted_snapshot_id integer;
begin
  select id from plan where plan.id = plan_id into validate_planid;
  if validate_planid is null then
    raise exception 'Plan % does not exist.', plan_id;
  end if;

  insert into plan_snapshot(plan_id, revision, name, duration, start_time)
    select id, revision, name, duration, start_time
    from plan where id = plan_id
    returning snapshot_id into inserted_snapshot_id;
  insert into plan_snapshot_activities(snapshot_id, id, name, tags, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments, last_modified_arguments_at, metadata)
    select
      inserted_snapshot_id,                                           --this is the snapshot id
      id, name, tags,source_scheduling_goal_id, created_at,   -- these are the rest of the data for an activity row
      last_modified_at, start_offset, type, arguments, last_modified_arguments_at, metadata
    from activity_directive where activity_directive.plan_id = create_snapshot.plan_id;
  return inserted_snapshot_id;
end;
$$;

-- when you take a snapshot of a plan, all of that plan's latest snapshots become the parent snapshots of the new snapshot.
create or replace function duplicate_plan(plan_id integer)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
begin

end;
$$;
