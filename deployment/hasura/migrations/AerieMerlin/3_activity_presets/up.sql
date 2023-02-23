/*
  This migration adds in the concept of activity presets.
  Presets are a collection of arguments that can be applied to an activity directive.
 */

-- Add Presets
create table activity_presets(
  id integer generated always as identity primary key,
  model_id integer not null,
  name text not null,
  associated_activity_type text not null,
  arguments merlin_argument_set not null,

  foreign key (model_id, associated_activity_type)
    references activity_type
    on delete cascade,
  unique (model_id, associated_activity_type, name)
);

comment on table activity_presets is e''
  'A set of arguments that can be applied to an activity of a given type.';

comment on column activity_presets.id is e''
  'The unique identifier for this activity preset';
comment on column activity_presets.model_id is e''
  'The model defining this activity preset is associated with.';
comment on column activity_presets.name is e''
  'The name of this activity preset, unique for an activity type within a mission model.';
comment on column activity_presets.associated_activity_type is e''
  'The activity type with which this activity preset is associated.';
comment on column activity_presets.arguments is e''
  'The set of arguments to be applied when this preset is applied.';

create table preset_to_directive(
  preset_id integer
    references activity_presets
    on update cascade
    on delete cascade,

  activity_id integer,
  plan_id integer,
  foreign key (activity_id, plan_id)
    references activity_directive
    on update cascade
    on delete cascade,

  constraint one_preset_per_activity_directive
    unique (activity_id, plan_id),

  primary key (preset_id, activity_id, plan_id)
);

comment on table preset_to_directive is e''
  'Associates presets with activity directives that have been assigned presets.';

create table preset_to_snapshot_directive(
  preset_id integer
    references activity_presets
    on update cascade
    on delete cascade,

  activity_id integer,
  snapshot_id integer,

  foreign key (activity_id, snapshot_id)
    references plan_snapshot_activities
    on update cascade
    on delete cascade,

  constraint one_preset_per_snapshot_directive
    unique (activity_id, snapshot_id),

  primary key (preset_id, activity_id, snapshot_id)
);

comment on table preset_to_snapshot_directive is e''
  'Associates presets with snapshot activity directives that have been assigned presets.';

-- Add Hasura Function
create function hasura_functions.apply_preset_to_activity(_preset_id int, _activity_id int, _plan_id int)
returns activity_directive
strict
language plpgsql as $$
  declare
    returning_directive activity_directive;
    ad_activity_type text;
    preset_activity_type text;
begin
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

-- Update snapshots
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
  insert into plan_snapshot_activities(
                snapshot_id, id, name, tags, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type,
                arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
                )
    select
      inserted_snapshot_id,                                   -- this is the snapshot id
      id, name, tags,source_scheduling_goal_id, created_at,   -- these are the rest of the data for an activity row
      last_modified_at, start_offset, type, arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from activity_directive where activity_directive.plan_id = create_snapshot.plan_id;
  insert into preset_to_snapshot_directive(preset_id, activity_id, snapshot_id)
  select ptd.preset_id, ptd.activity_id, inserted_snapshot_id
    from preset_to_directive ptd
    where ptd.plan_id = create_snapshot.plan_id;

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

create or replace function duplicate_plan(plan_id integer, new_plan_name text)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
  declare
    validate_plan_id integer;
    new_plan_id integer;
    created_snapshot_id integer;
begin
  select id from plan where plan.id = duplicate_plan.plan_id into validate_plan_id;
  if(validate_plan_id is null) then
    raise exception 'Plan % does not exist.', plan_id;
  end if;

  select create_snapshot(plan_id) into created_snapshot_id;

  insert into plan(revision, name, model_id, duration, start_time, parent_id)
    select
        0, new_plan_name, model_id, duration, start_time, plan_id
    from plan where id = plan_id
    returning id into new_plan_id;
  insert into activity_directive(
      id, plan_id, name, tags, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    )
    select
      id, new_plan_id, name, tags, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from activity_directive where activity_directive.plan_id = duplicate_plan.plan_id;
  insert into simulation (revision, simulation_template_id, plan_id, arguments)
    select 0, simulation_template_id, new_plan_id, arguments
    from simulation
    where simulation.plan_id = duplicate_plan.plan_id;

  insert into preset_to_directive(preset_id, activity_id, plan_id)
  select preset_id, activity_id, new_plan_id
  from preset_to_directive ptd where ptd.plan_id = duplicate_plan.plan_id;

  insert into plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end;
$$;

-- Plan Merge
create or replace procedure commit_merge(request_id integer)
  language plpgsql as $$
  declare
    validate_noConflicts integer;
    plan_id_R integer;
    snapshot_id_S integer;
begin
  if(select id from merge_request where id = request_id) is null then
    raise exception 'Invalid merge request id %.', request_id;
  end if;

  -- Stop if this merge is not 'in-progress'
  if (select status from merge_request where id = request_id) != 'in-progress' then
    raise exception 'Cannot commit a merge request that is not in-progress.';
  end if;

  -- Stop if any conflicts have not been resolved
  select * from conflicting_activities
  where merge_request_id = request_id and resolution = 'none'
  limit 1
  into validate_noConflicts;

  if(validate_noConflicts is not null) then
    raise exception 'There are unresolved conflicts in merge request %. Cannot commit merge.', request_id;
  end if;

  select plan_id_receiving_changes from merge_request mr where mr.id = request_id into plan_id_R;
  select snapshot_id_supplying_changes from merge_request mr where mr.id = request_id into snapshot_id_S;

  insert into merge_staging_area(
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type)
    -- gather delete data from the opposite tables
    select  commit_merge.request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
            start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'delete'::activity_change_type
      from  conflicting_activities ca
      join  activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = commit_merge.request_id
        and plan_id = plan_id_R
        and ca.change_type_supplying = 'delete'
    union
    select  commit_merge.request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
          start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'delete'::activity_change_type
      from  conflicting_activities ca
      join  plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = commit_merge.request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_receiving = 'delete'
    union
    select  commit_merge.request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
            start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'::activity_change_type
      from  conflicting_activities ca
      join  activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = commit_merge.request_id
        and plan_id = plan_id_R
        and ca.change_type_receiving = 'modify'
    union
    select  commit_merge.request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
          start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'modify'::activity_change_type
      from  conflicting_activities ca
      join  plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = commit_merge.request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_supplying = 'modify';

  -- Unlock so that updates can be written
  update plan
  set is_locked = false
  where id = plan_id_R;

  -- Update the plan's activities to match merge-staging-area's activities
  -- Add
  insert into activity_directive(
                id, plan_id, name, tags, source_scheduling_goal_id, created_at,
                start_offset, type, arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, name, tags, source_scheduling_goal_id, created_at,
            start_offset, type, arguments, metadata, anchor_id, anchored_to_start
   from merge_staging_area
  where merge_staging_area.merge_request_id = commit_merge.request_id
    and change_type = 'add';

  -- Modify
  insert into activity_directive(
    id, plan_id, "name", tags, source_scheduling_goal_id, created_at,
    start_offset, "type", arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, "name", tags, source_scheduling_goal_id, created_at,
          start_offset, "type", arguments, metadata, anchor_id, anchored_to_start
  from merge_staging_area
  where merge_staging_area.merge_request_id = commit_merge.request_id
    and change_type = 'modify'
  on conflict (id, plan_id)
  do update
  set name = excluded.name,
      tags = excluded.tags,
      source_scheduling_goal_id = excluded.source_scheduling_goal_id,
      created_at = excluded.created_at,
      start_offset = excluded.start_offset,
      type = excluded.type,
      arguments = excluded.arguments,
      metadata = excluded.metadata,
      anchor_id = excluded.anchor_id,
      anchored_to_start = excluded.anchored_to_start;

  -- Presets
  insert into preset_to_directive(preset_id, activity_id, plan_id)
  select pts.preset_id, pts.activity_id, plan_id_R
  from merge_staging_area msa, preset_to_snapshot_directive pts
  where msa.activity_id = pts.activity_id
    and msa.change_type = 'add'
     or msa.change_type = 'modify'
  on conflict (activity_id, plan_id)
    do update
    set preset_id = excluded.preset_id;

  -- Delete
  delete from activity_directive ad
  using merge_staging_area msa
  where ad.id = msa.activity_id
    and ad.plan_id = plan_id_R
    and msa.merge_request_id = commit_merge.request_id
    and msa.change_type = 'delete';

  -- Clean up
  delete from conflicting_activities where merge_request_id = request_id;
  delete from merge_staging_area where merge_staging_area.merge_request_id = commit_merge.request_id;

  update merge_request
  set status = 'accepted'
  where id = request_id;

  -- Attach snapshot history
  insert into plan_latest_snapshot(plan_id, snapshot_id)
  select plan_id_receiving_changes, snapshot_id_supplying_changes
  from merge_request
  where id = request_id;
end
$$;

call migrations.mark_migration_applied('3');
