-- add last_modified_by to activity_directive
alter table activity_directive
  add column last_modified_by text,

  add constraint activity_directive_last_modified_by_exists
    foreign key (last_modified_by)
    references metadata.users
    on update cascade
    on delete set null;

comment on column activity_directive.last_modified_by is e''
  'The user who last modified this activity_directive.';

-- add last_modified_by to activity_directive_extended view
drop view activity_directive_extended;
create view activity_directive_extended as
(
  select
    -- Activity Directive Properties
    ad.id as id,
    ad.plan_id as plan_id,
    -- Additional Properties
    ad.name as name,
    get_tags(ad.id, ad.plan_id) as tags,
    ad.source_scheduling_goal_id as source_scheduling_goal_id,
    ad.created_at as created_at,
    ad.last_modified_at as last_modified_at,
    ad.last_modified_by as last_modified_by,
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

-- redefine anchor deletion functions since we need to change one line
create or replace function hasura_functions.delete_activity_by_pk_reanchor_plan_start(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    _function_permission metadata.permission;
  begin
    _function_permission := metadata.get_function_permissions('delete_activity_reanchor_plan', hasura_session);
    perform metadata.raise_if_plan_merge_permission('delete_activity_reanchor_plan', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call metadata.check_general_permissions('delete_activity_reanchor_plan', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

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
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.last_modified_by, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
  end
$$;

create or replace function hasura_functions.delete_activity_by_pk_reanchor_to_anchor(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
  language plpgsql as $$
declare
    _function_permission metadata.permission;
begin
    _function_permission := metadata.get_function_permissions('delete_activity_reanchor', hasura_session);
    perform metadata.raise_if_plan_merge_permission('delete_activity_reanchor', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call metadata.check_general_permissions('delete_activity_reanchor', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

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
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.last_modified_by, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

create or replace function hasura_functions.delete_activity_by_pk_delete_subtree(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  volatile
  language plpgsql as $$
declare
  _function_permission metadata.permission;
begin
  _function_permission := metadata.get_function_permissions('delete_activity_subtree', hasura_session);
  perform metadata.raise_if_plan_merge_permission('delete_activity_subtree', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('delete_activity_subtree', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
  end if;

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
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.last_modified_by, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

alter table merge_staging_area
  add column last_modified_by text;

alter table plan_snapshot_activities
  add column last_modified_by text;

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
        select psa.id as activity_id, name, metadata.tag_ids_activity_snapshot(psa.id, merge_base_id),
               source_scheduling_goal_id, created_at, start_offset, type, arguments,
               metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities psa
        where psa.snapshot_id = merge_base_id
    intersect
      select id as activity_id, name, metadata.tag_ids_activity_snapshot(psa.id, snapshot_id_supplying),
             source_scheduling_goal_id, created_at, start_offset, type, arguments,
             metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities psa
        where psa.snapshot_id = snapshot_id_supplying) a;

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
        select id as activity_id, name, metadata.tag_ids_activity_snapshot(id, merge_base_id),
               source_scheduling_goal_id, created_at, start_offset, type, arguments,
               metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities psa
        where psa.snapshot_id = merge_base_id
        intersect
        select id as activity_id, name, metadata.tag_ids_activity_directive(id, plan_id_receiving),
               source_scheduling_goal_id, created_at, start_offset, type, arguments,
               metadata, anchor_id, anchored_to_start
        from activity_directive ad
        where ad.plan_id = plan_id_receiving) a;

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
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
         )
  -- 'adds' can go directly into the merge staging area table
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_snapshot(s_diff.activity_id, psa.snapshot_id),  source_scheduling_goal_id, created_at,
         last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
    from supplying_diff as  s_diff
    join plan_snapshot_activities psa
      on s_diff.activity_id = psa.id
    where snapshot_id = snapshot_id_supplying and change_type = 'add'
  union
  -- an 'add' between the receiving plan and merge base is actually a 'none'
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_directive(r_diff.activity_id, ad.plan_id),  source_scheduling_goal_id, created_at,
         last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'::activity_change_type
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
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
  )
  -- receiving 'none' and 'modify' against 'none' in the supplying side go into the merge staging area as 'none'
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_directive(diff_diff.activity_id, plan_id),  source_scheduling_goal_id, created_at,
         last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'
    from diff_diff
    join activity_directive
      on activity_id=id
    where plan_id = plan_id_receiving
      and change_type_supplying = 'none'
      and (change_type_receiving = 'modify' or change_type_receiving = 'none')
  union
  -- supplying 'modify' against receiving 'none' go into the merge staging area as 'modify'
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_snapshot(diff_diff.activity_id, snapshot_id),  source_scheduling_goal_id, created_at,
         last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join plan_snapshot_activities p
      on diff_diff.activity_id = p.id
    where snapshot_id = snapshot_id_supplying
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'modify')
  union
  -- supplying 'delete' against receiving 'none' go into the merge staging area as 'delete'
    select _merge_request_id, activity_id, name, metadata.tag_ids_activity_directive(diff_diff.activity_id, plan_id),  source_scheduling_goal_id, created_at,
         last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join activity_directive p
      on diff_diff.activity_id = p.id
    where plan_id = plan_id_receiving
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'delete');

  -- 'modify' against a 'modify' must be checked for equality first.
  with false_modify as (
    select activity_id, name, metadata.tag_ids_activity_directive(dd.activity_id, psa.snapshot_id) as tags,
           source_scheduling_goal_id, created_at, start_offset, type, arguments, metadata, anchor_id, anchored_to_start
    from plan_snapshot_activities psa
    join diff_diff dd
      on dd.activity_id = psa.id
    where psa.snapshot_id = snapshot_id_supplying
      and (dd.change_type_receiving = 'modify' and dd.change_type_supplying = 'modify')
    intersect
    select activity_id, name, metadata.tag_ids_activity_directive(dd.activity_id, ad.plan_id) as tags,
           source_scheduling_goal_id, created_at, start_offset, type, arguments, metadata, anchor_id, anchored_to_start
    from diff_diff dd
    join activity_directive ad
      on dd.activity_id = ad.id
    where ad.plan_id = plan_id_receiving
      and (dd.change_type_supplying = 'modify' and dd.change_type_receiving = 'modify'))
  insert into merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type)
  select _merge_request_id, ad.id, ad.name, tags,  ad.source_scheduling_goal_id, ad.created_at,
         ad.last_modified_by, ad.start_offset, ad.type, ad.arguments, ad.metadata, ad.anchor_id, ad.anchored_to_start, 'none'
  from false_modify fm left join activity_directive ad on (ad.plan_id, ad.id) = (plan_id_receiving, fm.activity_id);

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

create or replace procedure commit_merge(_request_id integer)
  language plpgsql as $$
  declare
    validate_noConflicts integer;
    plan_id_R integer;
    snapshot_id_S integer;
begin
  if(select id from merge_request where id = _request_id) is null then
    raise exception 'Invalid merge request id %.', _request_id;
  end if;

  -- Stop if this merge is not 'in-progress'
  if (select status from merge_request where id = _request_id) != 'in-progress' then
    raise exception 'Cannot commit a merge request that is not in-progress.';
  end if;

  -- Stop if any conflicts have not been resolved
  select * from conflicting_activities
  where merge_request_id = _request_id and resolution = 'none'
  limit 1
  into validate_noConflicts;

  if(validate_noConflicts is not null) then
    raise exception 'There are unresolved conflicts in merge request %. Cannot commit merge.', _request_id;
  end if;

  select plan_id_receiving_changes from merge_request mr where mr.id = _request_id into plan_id_R;
  select snapshot_id_supplying_changes from merge_request mr where mr.id = _request_id into snapshot_id_S;

  insert into merge_staging_area(
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type)
    -- gather delete data from the opposite tables
    select  _request_id, activity_id, name, metadata.tag_ids_activity_directive(ca.activity_id, ad.plan_id),
            source_scheduling_goal_id, created_at, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'delete'::activity_change_type
      from  conflicting_activities ca
      join  activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = _request_id
        and plan_id = plan_id_R
        and ca.change_type_supplying = 'delete'
    union
    select  _request_id, activity_id, name, metadata.tag_ids_activity_snapshot(ca.activity_id, psa.snapshot_id),
            source_scheduling_goal_id, created_at, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'delete'::activity_change_type
      from  conflicting_activities ca
      join  plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = _request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_receiving = 'delete'
    union
    select  _request_id, activity_id, name, metadata.tag_ids_activity_directive(ca.activity_id, ad.plan_id),
            source_scheduling_goal_id, created_at, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'none'::activity_change_type
      from  conflicting_activities ca
      join  activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = _request_id
        and plan_id = plan_id_R
        and ca.change_type_receiving = 'modify'
    union
    select  _request_id, activity_id, name, metadata.tag_ids_activity_snapshot(ca.activity_id, psa.snapshot_id),
            source_scheduling_goal_id, created_at, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'modify'::activity_change_type
      from  conflicting_activities ca
      join  plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = _request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_supplying = 'modify';

  -- Unlock so that updates can be written
  update plan
  set is_locked = false
  where id = plan_id_R;

  -- Update the plan's activities to match merge-staging-area's activities
  -- Add
  insert into activity_directive(
                id, plan_id, name, source_scheduling_goal_id, created_at, last_modified_by,
                start_offset, type, arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, name, source_scheduling_goal_id, created_at, last_modified_by,
            start_offset, type, arguments, metadata, anchor_id, anchored_to_start
   from merge_staging_area
  where merge_staging_area.merge_request_id = _request_id
    and change_type = 'add';

  -- Modify
  insert into activity_directive(
    id, plan_id, "name", source_scheduling_goal_id, created_at, last_modified_by,
    start_offset, "type", arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, "name", source_scheduling_goal_id, created_at, last_modified_by,
          start_offset, "type", arguments, metadata, anchor_id, anchored_to_start
  from merge_staging_area
  where merge_staging_area.merge_request_id = _request_id
    and change_type = 'modify'
  on conflict (id, plan_id)
  do update
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
    using merge_staging_area msa
    where adt.directive_id = msa.activity_id
      and adt.plan_id = plan_id_R
      and msa.merge_request_id = _request_id
      and msa.change_type = 'modify';

  insert into metadata.activity_directive_tags(plan_id, directive_id, tag_id)
    select plan_id_R, activity_id, t.id
    from merge_staging_area msa
    inner join metadata.tags t -- Inner join because it's specifically inserting into a tags-association table, so if there are no valid tags we do not want a null value for t.id
    on t.id = any(msa.tags)
    where msa.merge_request_id = _request_id
      and (change_type = 'modify'
       or change_type = 'add')
    on conflict (directive_id, plan_id, tag_id) do nothing;
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
    and msa.merge_request_id = _request_id
    and msa.change_type = 'delete';

  -- Clean up
  delete from conflicting_activities where merge_request_id = _request_id;
  delete from merge_staging_area where merge_staging_area.merge_request_id = _request_id;

  update merge_request
  set status = 'accepted'
  where id = _request_id;

  -- Attach snapshot history
  insert into plan_latest_snapshot(plan_id, snapshot_id)
  select plan_id_receiving_changes, snapshot_id_supplying_changes
  from merge_request
  where id = _request_id;
end
$$;

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

create or replace function duplicate_plan(_plan_id integer, new_plan_name text, new_owner text)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
  declare
    validate_plan_id integer;
    new_plan_id integer;
    created_snapshot_id integer;
begin
  select id from plan where plan.id = _plan_id into validate_plan_id;
  if(validate_plan_id is null) then
    raise exception 'Plan % does not exist.', _plan_id;
  end if;

  select create_snapshot(_plan_id) into created_snapshot_id;

  insert into plan(revision, name, model_id, duration, start_time, parent_id, owner, updated_by)
    select
        0, new_plan_name, model_id, duration, start_time, _plan_id, new_owner, new_owner
    from plan where id = _plan_id
    returning id into new_plan_id;
  insert into activity_directive(
      id, plan_id, name, source_scheduling_goal_id, created_at, last_modified_at, last_modified_by, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      id, new_plan_id, name, source_scheduling_goal_id, created_at, last_modified_at, last_modified_by, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from activity_directive where activity_directive.plan_id = _plan_id;

  with source_plan as (
    select simulation_template_id, arguments, simulation_start_time, simulation_end_time
    from simulation
    where simulation.plan_id = _plan_id
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
    from preset_to_directive ptd where ptd.plan_id = _plan_id;

  insert into metadata.plan_tags(plan_id, tag_id)
    select new_plan_id, tag_id
    from metadata.plan_tags pt where pt.plan_id = _plan_id;
  insert into metadata.activity_directive_tags(plan_id, directive_id, tag_id)
    select new_plan_id, directive_id, tag_id
    from metadata.activity_directive_tags adt where adt.plan_id = _plan_id;

  insert into plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end
$$;

create table activity_directive_changelog (
  revision integer not null,
  plan_id integer not null,
  activity_directive_id integer not null,

  name text,
  source_scheduling_goal_id integer,
  changed_at timestamptz not null default now(),
  changed_by text,
  start_offset interval not null,
  type text not null,
  arguments merlin_argument_set not null,
  changed_arguments_at timestamptz not null default now(),
  metadata merlin_activity_directive_metadata_set default '{}'::jsonb,

  anchor_id integer default null,
  anchored_to_start boolean default true not null,

  constraint activity_directive_changelog_natural_key
      primary key (plan_id, activity_directive_id, revision),
  constraint changelog_references_activity_directive
      foreign key (activity_directive_id, plan_id)
      references activity_directive
      on update cascade
      on delete cascade,
  constraint changed_by_exists
    foreign key (changed_by)
    references metadata.users
    on update cascade
    on delete set null
);

comment on table activity_directive_changelog is e''
    'A changelog that captures the 10 most recent revisions for each activity directive\n'
    'See activity_directive comments for descriptions of shared fields';

create function store_activity_directive_change()
  returns trigger
  language plpgsql as $$
begin
  insert into activity_directive_changelog (
    revision,
    plan_id,
    activity_directive_id,
    name,
    start_offset,
    type,
    arguments,
    changed_arguments_at,
    metadata,
    changed_by,
    anchor_id,
    anchored_to_start)
  values (
    (select coalesce(max(revision), -1) + 1
     from activity_directive_changelog
     where plan_id = new.plan_id
      and activity_directive_id = new.id),
    new.plan_id,
    new.id,
    new.name,
    new.start_offset,
    new.type,
    new.arguments,
    new.last_modified_arguments_at,
    new.metadata,
    new.last_modified_by,
    new.anchor_id,
    new.anchored_to_start);

  return new;
end
$$;

create trigger store_activity_directive_change_trigger
  after update or insert on activity_directive
  for each row
  execute function store_activity_directive_change();

create function delete_min_activity_directive_revision()
  returns trigger
  language plpgsql as $$
begin
  delete from activity_directive_changelog
  where activity_directive_id = new.activity_directive_id
    and plan_id = new.plan_id
    and revision = (select min(revision)
                    from activity_directive_changelog
                    where activity_directive_id = new.activity_directive_id
                      and plan_id = new.plan_id);
  return new;
end$$;

create trigger delete_min_activity_directive_revision_trigger
  after insert on activity_directive_changelog
  for each row
  when (new.revision > 10)
  execute function delete_min_activity_directive_revision();

create function hasura_functions.restore_activity_changelog(
  _plan_id integer,
  _activity_directive_id integer,
  _revision integer,
  hasura_session json
)
  returns setof activity_directive
  volatile
  language plpgsql as $$
declare
  _function_permission metadata.permission;
begin
  _function_permission :=
      metadata.get_function_permissions('restore_activity_changelog', hasura_session);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions(
      'restore_activity_changelog',
      _function_permission, _plan_id,
      (hasura_session ->> 'x-hasura-user-id')
    );
  end if;

  if not exists(select id from public.plan where id = _plan_id) then
    raise exception 'Plan % does not exist', _plan_id;
  end if;

  if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_directive_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_directive_id, _plan_id;
  end if;

  if not exists(select revision
                from public.activity_directive_changelog
                where (plan_id, activity_directive_id, revision) =
                      (_plan_id, _activity_directive_id, _revision))
  then
    raise exception 'Changelog Revision % does not exist for Plan % and Activity Directive %', _revision, _plan_id, _activity_directive_id;
  end if;

  return query
  update activity_directive as ad
  set name                       = c.name,
      source_scheduling_goal_id  = c.source_scheduling_goal_id,
      start_offset               = c.start_offset,
      type                       = c.type,
      arguments                  = c.arguments,
      last_modified_arguments_at = c.changed_arguments_at,
      metadata                   = c.metadata,
      anchor_id                  = c.anchor_id,
      anchored_to_start          = c.anchored_to_start,
      last_modified_at           = c.changed_at,
      last_modified_by           = c.changed_by
  from activity_directive_changelog as c
  where ad.id                    = _activity_directive_id
    and c.activity_directive_id  = _activity_directive_id
    and ad.plan_id               = _plan_id
    and c.plan_id                = _plan_id
    and c.revision               = _revision
  returning ad.*;
end
$$;

-- add new function key for restore_activity_changelog
alter type metadata.function_permission_key add value 'restore_activity_changelog';

-- add new key to user role
update metadata.user_role_permission
  set function_permissions = function_permissions
                               || jsonb_build_object('restore_activity_changelog', 'PLAN_OWNER_COLLABORATOR')
where role = 'user';

call migrations.mark_migration_applied('23');
