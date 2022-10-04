-- TODO list:
--   - duplicate function (done)
--        - duplicate temporal subset of plan
--   - merge request table
--   - diff function
--   - history tracking (tables and function)
--   - merge base function

-- Snapshot is a collection of the state of all the activities as they were at the time of the snapshot
-- as well as any other properties of the plan that can change
create table plan_snapshot(
  snapshot_id integer
    generated always as identity
    primary key,

  plan_id integer
    references plan
    on delete set null,

  revision integer not null,
  name text not null,
  duration interval not null,
  start_time timestamptz not null
);

create table plan_snapshot_activities(
   snapshot_id integer
      references plan_snapshot
      on delete cascade,
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
    metadata merlin_activity_directive_metadata_set,

    primary key (id, snapshot_id)
);

create table plan_snapshot_parent(
  snapshot_id integer
    references plan_snapshot,
  parent_snapshot_id integer
    references plan_snapshot,

  primary key (snapshot_id, parent_snapshot_id),
  constraint snapshot_cannot_be_own_parent
    check ( snapshot_id != parent_snapshot_id )
);

create table plan_latest_snapshot(
  plan_id integer,
  snapshot_id integer,

  primary key (plan_id, snapshot_id),
  foreign key (plan_id)
    references plan
    on update cascade
    on delete cascade,
  foreign key (snapshot_id)
    references plan_snapshot
    on update cascade
    on delete cascade
);

-- Captures the state of a plan and all of its activities
create function create_snapshot(plan_id integer)
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
      inserted_snapshot_id,                                   --this is the snapshot id
      id, name, tags,source_scheduling_goal_id, created_at,   -- these are the rest of the data for an activity row
      last_modified_at, start_offset, type, arguments, last_modified_arguments_at, metadata
    from activity_directive where activity_directive.plan_id = create_snapshot.plan_id;

  --all snapshots in plan_latest_snapshot for plan plan_id become the parent of the current snapshot
  insert into plan_snapshot_parent(snapshot_id, parent_snapshot_id)
    select inserted_snapshot_id, snapshot_id
    from plan_latest_snapshot where plan_latest_snapshot.plan_id = create_snapshot.plan_id;

  --remove all of those entries from plan_latest_snapshot and add this new snapshot.
  delete from plan_latest_snapshot where plan_latest_snapshot.plan_id = create_snapshot.plan_id;
  insert into plan_latest_snapshot(plan_id, snapshot_id) values (create_snapshot.plan_id, inserted_snapshot_id);

  return inserted_snapshot_id;
  end;
$$;

/*
  Copies all of a given plan's properties and activities into a new plan with the specified name.
  When duplicating a plan, a snapshot is created of the original plan.
  Additionally, that snapshot becomes the latest snapshot of the new plan.
*/
  create or replace function duplicate_plan(plan_id integer, new_plan_name text)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
begin

end;
$$;
