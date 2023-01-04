/*
  This migration adds in the concept of anchoring one activity directive's start time to another activity_directive's start or end time.
  Alternatively, an activity directive can be anchored to the plan's start or end time.
 */

/*
 Once https://github.com/NASA-AMMOS/aerie/issues/567 has been resolved, future migrations will only need to drop the relative columns,
 instead of dropping all columns and recreating them in the correct order (done in this fashion to limit the amount of functions dropped).

 Additionally, dropping these tables is only possible because they are return types and *do not* hold any data.
*/
alter table hasura_functions.begin_merge_return_value
  drop column merge_request_id,
  drop column non_conflicting_activities,
  drop column conflicting_activities;
alter table hasura_functions.get_non_conflicting_activities_return_value
  drop column activity_id,
  drop column change_type,
  drop column source,
  drop column target;
alter table hasura_functions.get_conflicting_activities_return_value
  drop column activity_id,
  drop column change_type_source,
  drop column change_type_target,
  drop column resolution,
  drop column source,
  drop column target,
  drop column merge_base;

-- Modify activity_directive
alter table activity_directive
  add column anchor_id integer default null,
  add column anchored_to_start boolean default true not null,
  drop constraint activity_directive_start_offset_is_nonnegative,
  add constraint anchor_in_plan
    foreign key (anchor_id, plan_id)
      references activity_directive
      on update cascade
      on delete restrict;

comment on column activity_directive.anchor_id is e''
  'The id of the activity_directive this activity_directive is anchored to. '
  'The value null indicates that this activity_directive is anchored to the plan.';
comment on column activity_directive.anchored_to_start is e''
  'If true, this activity_directive is anchored to the start time of its anchor. '
  'If false, this activity_directive is anchored to the end time of its anchor.';

-- Add rebasing functions
create function anchor_direct_descendents_to_plan(_activity_id int, _plan_id int)
  returns setof activity_directive
  language plpgsql as $$
declare
  _total_offset interval;
begin
  if _plan_id is null then
    raise exception 'Plan ID cannot be null.';
  end if;
  if _activity_id is null then
    raise exception 'Activity ID cannot be null.';
  end if;
  if not exists(select id from activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

  with recursive history(activity_id, anchor_id, total_offset) as (
    select ad.id, ad.anchor_id, ad.start_offset
    from activity_directive ad
    where (ad.id, ad.plan_id) = (_activity_id, _plan_id)
    union
    select ad.id, ad.anchor_id, h.total_offset + ad.start_offset
    from activity_directive ad, history h
    where (ad.id, ad.plan_id) = (h.anchor_id, _plan_id)
      and h.anchor_id is not null
  ) select total_offset
  from history
  where history.anchor_id is null
  into _total_offset;

  return query update activity_directive
    set start_offset = start_offset + _total_offset,
      anchor_id = null,
      anchored_to_start = true
    where (anchor_id, plan_id) = (_activity_id, _plan_id)
    returning *;
end
$$;
comment on function anchor_direct_descendents_to_plan(_activity_id integer, _plan_id integer) is e''
  'Given the primary key of an activity, reanchor all anchor chains attached to the activity to the plan.\n'
  'In the event of an end-time anchor, this function assumes all simulated activities have a duration of 0.';

create function anchor_direct_descendents_to_ancestor(_activity_id int, _plan_id int)
  returns setof activity_directive
  language plpgsql as $$
declare
  _current_offset interval;
  _current_anchor_id int;
begin
  if _plan_id is null then
    raise exception 'Plan ID cannot be null.';
  end if;
  if _activity_id is null then
    raise exception 'Activity ID cannot be null.';
  end if;
  if not exists(select id from activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

  select start_offset, anchor_id
  from activity_directive
  where (id, plan_id) = (_activity_id, _plan_id)
  into _current_offset, _current_anchor_id;

  return query
    update activity_directive
      set start_offset = start_offset + _current_offset,
        anchor_id = _current_anchor_id
      where (anchor_id, plan_id) = (_activity_id, _plan_id)
      returning *;
end
$$;
comment on function anchor_direct_descendents_to_ancestor(_activity_id integer, _plan_id integer) is e''
  'Given the primary key of an activity, reanchor all anchor chains attached to the activity to the anchor of said activity.\n'
  'In the event of an end-time anchor, this function assumes all simulated activities have a duration of 0.';

-- Track anchors in snapshots
alter table plan_snapshot_activities
  add column anchor_id integer default null,
  add column anchored_to_start boolean default true not null;

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

  insert into plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end;
$$;

-- Track anchors in plan merging
alter table merge_staging_area
  add column anchor_id integer default null,
  add column anchored_to_start boolean default true not null,
  drop constraint staging_area_start_offset_is_nonnegative;

comment on column merge_staging_area.anchor_id is e''
  'The identifier of the anchor of this activity directive to be committed.';
comment on column merge_staging_area.anchored_to_start is e''
  'The status of whether this activity directive is anchored to its anchor''s start time to be committed.';

-- Modify Begin_Merge and Commit_Merge
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

-- Add Anchor Validation
create table anchor_validation_status(
                                       activity_id integer not null,
                                       plan_id integer not null,
                                       reason_invalid text default null,
                                       primary key (activity_id, plan_id),
                                       foreign key (activity_id, plan_id)
                                         references activity_directive
                                         on update cascade
                                         on delete cascade
);

create index anchor_validation_plan_id_index on anchor_validation_status (plan_id);

comment on index anchor_validation_plan_id_index is e''
  'A similar index to that on activity_directive, as we often want to filter by plan_id';

comment on table anchor_validation_status is e''
  'The validation status of the anchor of a single activity_directive within a plan.';
comment on column anchor_validation_status.activity_id is e''
  'The synthetic identifier for the activity_directive.\n'
  'Unique within a given plan.';
comment on column anchor_validation_status.plan_id is e''
  'The plan within which the activity_directive is located';
comment on column anchor_validation_status.reason_invalid is e''
  'If null, the anchor is valid. If not null, this contains a reason why the anchor is invalid.';

/*
    An activity directive may have a negative offset from its anchor's start time.
    If its anchor is anchored to the end time of another activity (or so on up the chain), the activity with a
    negative offset must come out to have a positive offset relative to that end time anchor.
*/
create procedure validate_nonnegative_net_end_offset(_activity_id integer, _plan_id integer)
  security definer
  language plpgsql as $$
declare
  end_anchor_id integer;
  offset_from_end_anchor interval;
  _anchor_id integer;
  _start_offset interval;
  _anchored_to_start boolean;
begin
  select anchor_id, start_offset, anchored_to_start
  from activity_directive
  where (id, plan_id) = (_activity_id, _plan_id)
  into _anchor_id, _start_offset, _anchored_to_start;

  if (_anchor_id is not null)           -- if the activity is anchored to the plan, then it can't be anchored to the end of another activity directive
  then
    /*
      Postgres ANDs don't "short-circuit" -- all clauses are evaluated. Therefore, this query is placed here so that
      it only runs iff the outer 'if' is true
    */
    with recursive end_time_anchor(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
      select _activity_id, _anchor_id, _anchored_to_start, _start_offset, _start_offset
      union
      select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, eta.total_offset + ad.start_offset
      from activity_directive ad, end_time_anchor eta
      where (ad.id, ad.plan_id) = (eta.anchor_id, _plan_id)
        and eta.anchor_id is not null                               -- stop at plan
        and eta.anchored_to_start                                   -- or stop at end time anchor
    ) select into end_anchor_id, offset_from_end_anchor
        anchor_id, total_offset from end_time_anchor eta -- get the id of the activity that the selected activity is anchored to
    where not eta.anchored_to_start and eta.anchor_id is not null
    limit 1;

    if end_anchor_id is not null and offset_from_end_anchor < '0' then
      raise notice 'Activity Directive % has a net negative offset relative to an end-time anchor on Activity Directive %.', _activity_id, end_anchor_id;

      insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
      values (_activity_id, _plan_id, 'Activity Directive ' || _activity_id || ' has a net negative offset relative to an end-time' ||
                                      ' anchor on Activity Directive ' || end_anchor_id ||'.')
      on conflict (activity_id, plan_id) do update
        set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to an end-time' ||
                             ' anchor on Activity Directive ' || end_anchor_id ||'.';
    end if;
  end if;
end
$$;
comment on procedure validate_nonnegative_net_end_offset(_activity_id integer, _plan_id integer) is e''
  'Returns true if the specified activity has a net negative offset from a non-plan activity end-time anchor. Otherwise, returns false.\n'
  'If true, writes to anchor_validation_status.';

create procedure validate_nonegative_net_plan_start(_activity_id integer, _plan_id integer)
  security definer
  language plpgsql as $$
declare
  net_offset interval;
  _anchor_id integer;
  _start_offset interval;
  _anchored_to_start boolean;
begin
  select anchor_id, start_offset, anchored_to_start
  from activity_directive
  where (id, plan_id) = (_activity_id, _plan_id)
  into _anchor_id, _start_offset, _anchored_to_start;

  if (_start_offset < '0' and _anchored_to_start) then -- only need to check if anchored to start or something with a negative offset
    with recursive anchors(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
      select _activity_id, _anchor_id, _anchored_to_start, _start_offset, _start_offset
      union
      select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, anchors.total_offset + ad.start_offset
      from activity_directive ad, anchors
      where anchors.anchor_id is not null                               -- stop at plan
        and  (ad.id, ad.plan_id) = (anchors.anchor_id, _plan_id)
        and anchors.anchored_to_start                                  -- or, stop at end-time offset
    )
    select total_offset  -- get the id of the activity that the selected activity is anchored to
    from anchors a
    where a.anchor_id is null
      and a.anchored_to_start
    limit 1
    into net_offset;

    if(net_offset < '0') then
      raise notice 'Activity Directive % has a net negative offset relative to Plan Start.', _activity_id;

      insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
      values (_activity_id, _plan_id, 'Activity Directive ' || _activity_id || ' has a net negative offset relative to Plan Start.')
      on conflict (activity_id, plan_id) do update
        set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to Plan Start.';
    end if;
  end if;
end
$$;
comment on procedure validate_nonegative_net_plan_start(_activity_id integer, _plan_id integer) is e''
  'Returns true if the specified activity has a net negative offset from plan start. Otherwise, returns false.\n'
  'If true, writes to anchor_validation_status.';

create function validate_anchors()
  returns trigger
  security definer
  language plpgsql as $$
declare
  end_anchor_id integer;
  invalid_descendant_act_ids integer[];
  offset_from_end_anchor interval;
  offset_from_plan_start interval;
begin
  -- Clear the reason invalid field (if an exception is thrown, this will be rolled back)
  insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
  values (new.id, new.plan_id, '')
  on conflict (activity_id, plan_id) do update
    set reason_invalid = '';

  -- An activity cannot anchor to itself
  if(new.anchor_id = new.id) then
    raise exception 'Cannot anchor activity % to itself.', new.anchor_id;
  end if;

  -- Validate that no cycles were added
  if exists(
      with recursive history(activity_id, anchor_id, is_cycle, path) as (
        select new.id, new.anchor_id, false, array[new.id]
        union all
        select ad.id, ad.anchor_id,
               ad.id = any(path),
               path || ad.id
        from activity_directive ad, history h
        where (ad.id, ad.plan_id) = (h.anchor_id, new.plan_id)
          and not is_cycle
      ) select * from history
      where is_cycle
      limit 1
    ) then
    raise exception 'Cycle detected. Cannot apply changes.';
  end if;

  /*
    An activity directive may have a negative offset from its anchor's start time.
    If its anchor is anchored to the end time of another activity (or so on up the chain), the activity with a
    negative offset must come out to have a positive offset relative to that end time anchor.
  */
  call validate_nonnegative_net_end_offset(new.id, new.plan_id);
  call validate_nonegative_net_plan_start(new.id, new.plan_id);

  /*
    Everything below validates that the activities anchored to this one did not become invalid as a result of these changes.

    This only checks descendent start-time anchors, as we know that the state after an end-time anchor is valid
    (As if it no longer is, it will be caught when that activity's row is processed by this trigger)
  */
  -- Get collection of dependent activities, with offset relative to this activity
  create temp table dependent_activities as
  with recursive d_activities(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
    select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, ad.start_offset
    from activity_directive ad
    where (ad.anchor_id, ad.plan_id) = (new.id, new.plan_id) -- select all activities anchored to this one
    union
    select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, da.total_offset + ad.start_offset
    from activity_directive ad, d_activities da
    where (ad.anchor_id, ad.plan_id) = (da.activity_id, new.plan_id) -- select all activities anchored to those in the selection
      and ad.anchored_to_start  -- stop at next end-time anchor
  ) select activity_id, total_offset
  from d_activities da;

  -- Get the total offset from the most recent end-time anchor earlier in this activity's chain (or null if there is none)
  with recursive end_time_anchor(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
    select new.id, new.anchor_id, new.anchored_to_start, new.start_offset, new.start_offset
    union
    select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, eta.total_offset + ad.start_offset
    from activity_directive ad, end_time_anchor eta
    where (ad.id, ad.plan_id) = (eta.anchor_id, new.plan_id)
      and eta.anchor_id is not null                               -- stop at plan
      and eta.anchored_to_start                                   -- or stop at end time anchor
  ) select into end_anchor_id, offset_from_end_anchor
      anchor_id, total_offset from end_time_anchor eta -- get the id of the activity that the selected activity is anchored to
  where not eta.anchored_to_start and eta.anchor_id is not null
  limit 1;

  -- Not null iff the activity being looked at has some end anchor to another activity in its chain
  if offset_from_end_anchor is not null then
    select array_agg(activity_id) from dependent_activities
    where total_offset + offset_from_end_anchor < '0'
    into invalid_descendant_act_ids;

    if invalid_descendant_act_ids is not null then
      raise info 'The following Activity Directives now have a net negative offset relative to an end-time anchor on Activity Directive %: % \n'
        'There may be additional activities that are invalid relative to this activity.',
        end_anchor_id, array_to_string(invalid_descendant_act_ids, ',');

      insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
      select id, new.plan_id, 'Activity Directive ' || id || ' has a net negative offset relative to an end-time' ||
                              ' anchor on Activity Directive ' || end_anchor_id ||'.'
      from unnest(invalid_descendant_act_ids) as id
      on conflict (activity_id, plan_id) do update
        set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to an end-time' ||
                             ' anchor on Activity Directive ' || end_anchor_id ||'.';
    end if;
  end if;

  -- Gets the total offset from plan start (or null if there's an end-time anchor in the way)
  with recursive anchors(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
    select new.id, new.anchor_id, new.anchored_to_start, new.start_offset, new.start_offset
    union
    select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, anchors.total_offset + ad.start_offset
    from activity_directive ad, anchors
    where anchors.anchor_id is not null                               -- stop at plan
      and (ad.id, ad.plan_id) = (anchors.anchor_id, new.plan_id)
      and anchors.anchored_to_start                                  -- or, stop at end-time offset
  )
  select total_offset  -- get the id of the activity that the selected activity is anchored to
  from anchors a
  where a.anchor_id is null
    and a.anchored_to_start
  limit 1
  into offset_from_plan_start;

  -- Not null iff the activity being looked at is connected to plan start via a chain of start anchors
  if offset_from_plan_start is not null then
    -- Validate descendents
    invalid_descendant_act_ids := null;
    select array_agg(activity_id) from dependent_activities
    where total_offset + offset_from_plan_start < '0' into invalid_descendant_act_ids;  -- grab all and split

    if invalid_descendant_act_ids is not null then
      raise info 'The following Activity Directives now have a net negative offset relative to Plan Start: % \n'
        'There may be additional activities that are invalid relative to this activity.',
        array_to_string(invalid_descendant_act_ids, ',');

      insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
      select id, new.plan_id, 'Activity Directive ' || id || ' has a net negative offset relative to Plan Start.'
      from unnest(invalid_descendant_act_ids) as id
      on conflict (activity_id, plan_id) do update
        set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to Plan Start.';
    end if;
  end if;

  -- These are both null iff the activity is anchored to plan end
  if(offset_from_plan_start is null and offset_from_end_anchor is null) then
    -- All dependent activities should have no errors, as Plan End can have an offset of any value.
    insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
    select da.activity_id, new.plan_id, ''
    from dependent_activities as da
    on conflict (activity_id, plan_id) do update
      set reason_invalid = '';
  end if;

  -- Remove the error from the dependent activities that wouldn't have been flagged by the earlier checks.
  insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
  select da.activity_id, new.plan_id, ''
  from dependent_activities as da
  where total_offset + offset_from_plan_start >= '0'
     or total_offset + offset_from_end_anchor >= '0' -- only one of these checks will run depending on which one has `null` behind the offset
  on conflict (activity_id, plan_id) do update
    set reason_invalid = '';

  drop table dependent_activities;
  return new;
end $$;

create constraint trigger validate_anchors_update_trigger
  after update
  on activity_directive
  deferrable initially deferred
  for each row
  when (old.anchor_id is distinct from new.anchor_id -- != but allows for one side to be null
    or old.anchored_to_start != new.anchored_to_start
    or old.start_offset != new.start_offset)
execute procedure validate_anchors();

create constraint trigger validate_anchors_insert_trigger
  after insert
  on activity_directive
  deferrable initially deferred
  for each row
execute procedure validate_anchors();

-- Hasura functions for handling anchors during delete
create table hasura_functions.delete_anchor_return_value(
                                                          affected_row activity_directive,
                                                          change_type text
);

create function hasura_functions.delete_activity_by_pk_reanchor_plan_start(_activity_id int, _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  language plpgsql as $$
begin
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
    select (deleted.id, deleted.plan_id, deleted.name, deleted.tags, deleted.source_scheduling_goal_id,
            deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
            deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

create function hasura_functions.delete_activity_by_pk_reanchor_to_anchor(_activity_id int, _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  language plpgsql as $$
begin
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
    select (deleted.id, deleted.plan_id, deleted.name, deleted.tags, deleted.source_scheduling_goal_id,
            deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
            deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

create function hasura_functions.delete_activity_by_pk_delete_subtree(_activity_id int, _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  language plpgsql as $$
begin
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
    select (deleted.id, deleted.plan_id, deleted.name, deleted.tags, deleted.source_scheduling_goal_id,
            deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
            deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

-- Add back the dropped tables in reverse order
alter table hasura_functions.get_conflicting_activities_return_value
  add column activity_id integer,
  add column change_type_source activity_change_type,
  add column change_type_target activity_change_type,
  add column resolution resolution_type,
  add column source plan_snapshot_activities,
  add column target activity_directive,
  add column merge_base plan_snapshot_activities;
alter table hasura_functions.get_non_conflicting_activities_return_value
  add column activity_id integer,
  add column change_type activity_change_type,
  add column source plan_snapshot_activities,
  add column target activity_directive;
alter table hasura_functions.begin_merge_return_value
  add column merge_request_id integer,
  add column non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[],
  add column conflicting_activities hasura_functions.get_conflicting_activities_return_value[];

call migrations.mark_migration_applied('1');
