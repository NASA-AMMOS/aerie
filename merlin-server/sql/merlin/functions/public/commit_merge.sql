/*
  Commit merge takes all of the contents of the staging area and all of the resolved conflicts
  and applies the changes to the plan getting merged into.
 */
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
