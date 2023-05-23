-- Commit Merge
drop procedure commit_merge(_request_id integer);
create procedure commit_merge(request_id integer)
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

-- Begin Merge
create or replace procedure begin_merge(_merge_request_id integer, review_username text)
  language plpgsql as $$
  declare
    validate_id integer;
    validate_status merge_request_status;
    validate_non_no_op_status activity_change_type;
    snapshot_id_supplying integer;
    plan_id_receiving integer;
    merge_base_id integer;
begin
  -- validate id and status
  select id, status
    from merge_request
    where _merge_request_id = id
    into validate_id, validate_status;

  if validate_id is null then
    raise exception 'Request ID % is not present in merge_request table.', _merge_request_id;
  end if;

  if validate_status != 'pending' then
    raise exception 'Cannot begin request. Merge request % is not in pending state.', _merge_request_id;
  end if;

  -- select from merge-request the snapshot_sc (s_sc) and plan_rc (p_rc) ids
  select plan_id_receiving_changes, snapshot_id_supplying_changes
    from merge_request
    where id = _merge_request_id
    into plan_id_receiving, snapshot_id_supplying;

  -- ensure the plan receiving changes isn't locked
  if (select is_locked from plan where plan.id=plan_id_receiving) then
    raise exception 'Cannot begin merge request. Plan to receive changes is locked.';
  end if;

  -- lock plan_rc
  update plan
    set is_locked = true
    where plan.id = plan_id_receiving;

  -- get merge base (mb)
  select get_merge_base(plan_id_receiving, snapshot_id_supplying)
    into merge_base_id;

  -- update the status to "in progress"
  update merge_request
    set status = 'in-progress',
    merge_base_snapshot_id = merge_base_id,
    reviewer_username = review_username
    where id = _merge_request_id;


  -- perform diff between mb and s_sc (s_diff)
    -- delete is B minus A on key
    -- add is A minus B on key
    -- A intersect B is no op
    -- A minus B on everything except everything currently in the table is modify
  create temp table supplying_diff(
    activity_id integer,
    change_type activity_change_type not null
  );

  insert into supplying_diff (activity_id, change_type)
  select activity_id, 'delete'
  from(
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = merge_base_id
    except
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = snapshot_id_supplying) a;

  insert into supplying_diff (activity_id, change_type)
  select activity_id, 'add'
  from(
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = snapshot_id_supplying
    except
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = merge_base_id) a;

  insert into supplying_diff (activity_id, change_type)
    select activity_id, 'none'
      from(
        select id as activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_at,
            start_offset, type, arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities
        where snapshot_id = merge_base_id
    intersect
      select id as activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_at,
            start_offset, type, arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities
        where snapshot_id = snapshot_id_supplying) a;

  insert into supplying_diff (activity_id, change_type)
    select activity_id, 'modify'
    from(
      select id as activity_id from plan_snapshot_activities
        where snapshot_id = merge_base_id or snapshot_id = snapshot_id_supplying
      except
      select activity_id from supplying_diff) a;

  -- perform diff between mb and p_rc (r_diff)
  create temp table receiving_diff(
     activity_id integer,
     change_type activity_change_type not null
  );

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'delete'
  from(
        select id as activity_id
        from plan_snapshot_activities
        where snapshot_id = merge_base_id
        except
        select id as activity_id
        from activity_directive
        where plan_id = plan_id_receiving) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'add'
  from(
        select id as activity_id
        from activity_directive
        where plan_id = plan_id_receiving
        except
        select id as activity_id
        from plan_snapshot_activities
        where snapshot_id = merge_base_id) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'none'
  from(
        select id as activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_at,
               start_offset, type, arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities
        where snapshot_id = merge_base_id
        intersect
        select id as activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_at,
               start_offset, type, arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from activity_directive
        where plan_id = plan_id_receiving) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'modify'
  from (
        (select id as activity_id
         from plan_snapshot_activities
         where snapshot_id = merge_base_id
         union
         select id as activity_id
         from activity_directive
         where plan_id = plan_id_receiving)
        except
        select activity_id
        from receiving_diff) a;


  -- perform diff between s_diff and r_diff
      -- upload the non-conflicts into merge_staging_area
      -- upload conflict into conflicting_activities
  create temp table diff_diff(
    activity_id integer,
    change_type_supplying activity_change_type not null,
    change_type_receiving activity_change_type not null
  );

  -- this is going to require us to do the "none" operation again on the remaining modifies
  -- but otherwise we can just dump the 'adds' and 'none' into the merge staging area table

  -- 'delete' against a 'delete' does not enter the merge staging area table
  -- receiving 'delete' against supplying 'none' does not enter the merge staging area table

  insert into merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
         )
  -- 'adds' can go directly into the merge staging area table
  select _merge_request_id, activity_id, name, tags,  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
    from supplying_diff as  s_diff
    join plan_snapshot_activities psa
      on s_diff.activity_id = psa.id
    where snapshot_id = snapshot_id_supplying and change_type = 'add'
  union
  -- an 'add' between the receiving plan and merge base is actually a 'none'
  select _merge_request_id, activity_id, name, tags,  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'::activity_change_type
    from receiving_diff as r_diff
    join activity_directive ad
      on r_diff.activity_id = ad.id
    where plan_id = plan_id_receiving and change_type = 'add';

  -- put the rest in diff_diff
  insert into diff_diff (activity_id, change_type_supplying, change_type_receiving)
  select activity_id, supplying_diff.change_type as change_type_supplying, receiving_diff.change_type as change_type_receiving
    from receiving_diff
    join supplying_diff using (activity_id)
  where receiving_diff.change_type != 'add' or supplying_diff.change_type != 'add';

  -- ...except for that which is not recorded
  delete from diff_diff
    where (change_type_receiving = 'delete' and  change_type_supplying = 'delete')
       or (change_type_receiving = 'delete' and change_type_supplying = 'none');

  insert into merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
  )
  -- receiving 'none' and 'modify' against 'none' in the supplying side go into the merge staging area as 'none'
  select _merge_request_id, activity_id, name, tags,  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'
    from diff_diff
    join activity_directive
      on activity_id=id
    where plan_id = plan_id_receiving
      and change_type_supplying = 'none'
      and (change_type_receiving = 'modify' or change_type_receiving = 'none')
  union
  -- supplying 'modify' against receiving 'none' go into the merge staging area as 'modify'
  select _merge_request_id, activity_id, name, tags,  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join plan_snapshot_activities p
      on diff_diff.activity_id = p.id
    where snapshot_id = snapshot_id_supplying
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'modify')
  union
  -- supplying 'delete' against receiving 'none' go into the merge staging area as 'delete'
    select _merge_request_id, activity_id, name, tags,  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join activity_directive p
      on diff_diff.activity_id = p.id
    where plan_id = plan_id_receiving
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'delete')
  union
  -- 'modify' against a 'modify' must be checked for equality first.
  select _merge_request_id, activity_id, name, tags,  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'
  from (
    select activity_id, name, tags,  source_scheduling_goal_id, created_at,
           start_offset, type, arguments, metadata, anchor_id, anchored_to_start
      from plan_snapshot_activities psa
      join diff_diff dd
        on dd.activity_id = psa.id
      where psa.snapshot_id = snapshot_id_supplying
        and (dd.change_type_receiving = 'modify' and dd.change_type_supplying = 'modify')
    intersect
    select activity_id, name, tags,  source_scheduling_goal_id, created_at,
           start_offset, type, arguments, metadata, anchor_id, anchored_to_start
      from diff_diff dd
      join activity_directive ad
        on dd.activity_id = ad.id
      where ad.plan_id = plan_id_receiving
        and (dd.change_type_supplying = 'modify' and dd.change_type_receiving = 'modify')
  ) a;

  -- 'modify' against 'delete' and inequal 'modify' against 'modify' goes into conflict table (aka everything left in diff_diff)
  insert into conflicting_activities (merge_request_id, activity_id, change_type_supplying, change_type_receiving)
  select begin_merge._merge_request_id, activity_id, change_type_supplying, change_type_receiving
  from (select begin_merge._merge_request_id, activity_id
        from diff_diff
        except
        select msa.merge_request_id, activity_id
        from merge_staging_area msa) a
  join diff_diff using (activity_id);

  -- Fail if there are no differences between the snapshot and the plan getting merged
  validate_non_no_op_status := null;
  select change_type_receiving
  from conflicting_activities
  where merge_request_id = _merge_request_id
  limit 1
  into validate_non_no_op_status;

  if validate_non_no_op_status is null then
    select change_type
    from merge_staging_area msa
    where merge_request_id = _merge_request_id
    and msa.change_type != 'none'
    limit 1
    into validate_non_no_op_status;

    if validate_non_no_op_status is null then
      raise exception 'Cannot begin merge. The contents of the two plans are identical.';
    end if;
  end if;


  -- clean up
  drop table supplying_diff;
  drop table receiving_diff;
  drop table diff_diff;
end
$$;

-- Tag Helper Functions
drop function metadata.tag_ids_activity_directive(_directive_id integer, _plan_id integer);
drop function metadata.tag_ids_activity_snapshot(_directive_id integer, _snapshot_id integer);

-- Duplicate Plan
drop function duplicate_plan(integer, text, text);
create function duplicate_plan(plan_id integer, new_plan_name text, new_owner text)
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

  insert into plan(revision, name, model_id, duration, start_time, parent_id, owner)
    select
        0, new_plan_name, model_id, duration, start_time, plan_id, new_owner
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

  with source_plan as (
    select simulation_template_id, arguments, simulation_start_time, simulation_end_time
    from simulation
    where simulation.plan_id = duplicate_plan.plan_id
  )
  update simulation s
  set simulation_template_id = source_plan.simulation_template_id,
      arguments = source_plan.arguments,
      simulation_start_time = source_plan.simulation_start_time,
      simulation_end_time = source_plan.simulation_end_time
  from source_plan
  where s.plan_id = new_plan_id;

  insert into preset_to_directive(preset_id, activity_id, plan_id)
  select preset_id, activity_id, new_plan_id
  from preset_to_directive ptd where ptd.plan_id = duplicate_plan.plan_id;

  insert into plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end
$$;

-- Create Snapshot
comment on function create_snapshot(integer) is null;
drop function create_snapshot(_plan_id integer);
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

-- Merge Staging Area
alter table merge_staging_area
  add column tags2 text[] default '{}';

with tag_names as (
  select msa.activity_id, msa.merge_request_id, array_agg(t.name) as tags
  from merge_staging_area msa
  inner join metadata.tags t -- We don't want a `null` when there is no correlation
    on t.id = any(msa.tags)
  group by msa.activity_id, msa.merge_request_id)
update public.merge_staging_area msa
set tags2 = tag_names.tags
from tag_names
where msa.activity_id = tag_names.activity_id
  and msa.merge_request_id = tag_names.merge_request_id;

alter table merge_staging_area
  drop column tags;
alter table merge_staging_area
  rename column tags2 to tags;

-- "Unlock" Activity Directive and Snapshot Directive
alter table hasura_functions.begin_merge_return_value
  drop column non_conflicting_activities,
  drop column conflicting_activities;
alter table hasura_functions.get_non_conflicting_activities_return_value
   drop column source,
   drop column target;
alter table hasura_functions.get_conflicting_activities_return_value
   drop column source,
   drop column target,
   drop column merge_base;
alter table hasura_functions.delete_anchor_return_value
  drop column affected_row;

-- Activity Directives
alter table public.activity_directive
  add column tags text[] default '{}';
comment on column activity_directive.tags is e''
  'The tags associated with this activity_directive.';

with directive_tags as (
  select directive_id, plan_id, array_agg(name) as tags
  from metadata.activity_directive_tags adt, metadata.tags t
  where t.id = adt.tag_id
  group by directive_id, plan_id)
update public.activity_directive ad
set tags = dt.tags
from directive_tags dt
where ad.id = dt.directive_id
  and ad.plan_id = dt.plan_id;

-- Activity Directive Extended View
drop view activity_directive_extended;
create view activity_directive_extended as
(
  select
    -- Activity Directive Properties
    ad.id as id,
    ad.plan_id as plan_id,
    -- Additional Properties
    ad.name as name,
    ad.tags,
    ad.source_scheduling_goal_id as source_scheduling_goal_id,
    ad.created_at as created_at,
    ad.last_modified_at as last_modified_at,
    ad.start_offset as start_offset,
    ad.type as type,
    ad.arguments as arguments,
    ad.last_modified_arguments_at as last_modified_arguments_at,
    ad.metadata as metadata,
    ad.anchor_id as anchor_id,
    ad.anchored_to_start as anchored_to_start,
    -- Derived Properties
    get_approximate_start_time(ad.id, ad.plan_id) as approximate_start_time,
    ptd.preset_id as preset_id,
    ap.arguments as preset_arguments
   from activity_directive ad
   left join preset_to_directive ptd on ad.id = ptd.activity_id and ad.plan_id = ptd.plan_id
   left join activity_presets ap on ptd.preset_id = ap.id
);
drop function get_tags(_activity_id int, _plan_id int);

comment on table metadata.activity_directive_tags is null;
drop table metadata.activity_directive_tags;

-- Snapshot Activity Tags
alter table plan_snapshot_activities
add column tags text[];

with snapshot_tags as (
  select directive_id, snapshot_id, array_agg(name) as tags
  from metadata.snapshot_activity_tags sat, metadata.tags t
  where t.id = sat.tag_id
  group by directive_id, snapshot_id)
update public.plan_snapshot_activities psa
set tags = st.tags
from snapshot_tags st
where psa.id = st.directive_id
  and psa.snapshot_id = st.snapshot_id;

comment on table metadata.snapshot_activity_tags is null;
drop table metadata.snapshot_activity_tags;

-- "Lock" Activity Directive and Snapshot Directive
alter table hasura_functions.delete_anchor_return_value
  add column affected_row activity_directive;
alter table hasura_functions.get_conflicting_activities_return_value
   add column source plan_snapshot_activities,
   add column target activity_directive,
   add column merge_base plan_snapshot_activities;
alter table hasura_functions.get_non_conflicting_activities_return_value
   add column source plan_snapshot_activities,
   add column target activity_directive;
alter table hasura_functions.begin_merge_return_value
  add column non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[],
  add column conflicting_activities hasura_functions.get_conflicting_activities_return_value[];

-- Constraint Tags
alter table public."constraint"
  add column tags text[] default '{}';
comment on column "constraint".tags is e''
  'The tags associated with this constraint.';

with constraint_tags as (
  select ct.constraint_id, array_agg(t.name) as tags
  from metadata.constraint_tags ct, metadata.tags t
  where ct.tag_id = t.id
  group by constraint_id)
update public."constraint" c
set tags = ct.tags
from constraint_tags ct
where c.id = ct.constraint_id;

comment on table metadata.constraint_tags is null;
drop table metadata.constraint_tags;

-- Activity Type
alter table public.activity_type
add column subsystem2 text;

update public.activity_type at
set subsystem2 = t.name
from metadata.tags t
where t.id = at.subsystem;

alter table public.activity_type
drop column subsystem;

alter table public.activity_type
rename column subsystem2 to subsystem;

-- Plan Tags
comment on table metadata.plan_tags is null;
drop table metadata.plan_tags;

-- Tags
comment on column metadata.tags.created_at is null;
comment on column metadata.tags.owner is null;
comment on column metadata.tags.color is null;
comment on column metadata.tags.name is null;
comment on column metadata.tags.id is null;
comment on table metadata.tags is null;

drop table metadata.tags;
drop schema metadata;

call migrations.mark_migration_rolled_back('16');
