-- up.sql creates table and sequence, delete them
DROP TABLE ui.seen_sources CASCADE;
DROP TABLE merlin.plan_derivation_group CASCADE;
DROP TABLE merlin.external_event CASCADE;
DROP TABLE merlin.external_source CASCADE;
DROP TABLE merlin.external_source_type CASCADE;
DROP TABLE merlin.external_event_type CASCADE;
DROP TABLE merlin.derivation_group CASCADE;
DROP FUNCTION merlin.subtract_later_ranges CASCADE;
DROP FUNCTION merlin.check_event_times CASCADE;

/*
  Commit merge takes all of the contents of the staging area and all of the resolved conflicts
  and applies the changes to the plan getting merged into.
 */
create or replace procedure merlin.commit_merge(_request_id integer)
  language plpgsql as $$
  declare
    validate_noConflicts integer;
    plan_id_R integer;
    snapshot_id_S integer;
begin
  if(select id from merlin.merge_request where id = _request_id) is null then
    raise exception 'Invalid merge request id %.', _request_id;
  end if;

  -- Stop if this merge is not 'in-progress'
  if (select status from merlin.merge_request where id = _request_id) != 'in-progress' then
    raise exception 'Cannot commit a merge request that is not in-progress.';
  end if;

  -- Stop if any conflicts have not been resolved
  select * from merlin.conflicting_activities
  where merge_request_id = _request_id and resolution = 'none'
  limit 1
  into validate_noConflicts;

  if(validate_noConflicts is not null) then
    raise exception 'There are unresolved conflicts in merge request %. Cannot commit merge.', _request_id;
  end if;

  select plan_id_receiving_changes from merlin.merge_request mr where mr.id = _request_id into plan_id_R;
  select snapshot_id_supplying_changes from merlin.merge_request mr where mr.id = _request_id into snapshot_id_S;

  insert into merlin.merge_staging_area(
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, created_by, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type)
    -- gather delete data from the opposite tables
    select  _request_id, activity_id, name, tags.tag_ids_activity_directive(ca.activity_id, ad.plan_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'delete'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = _request_id
        and plan_id = plan_id_R
        and ca.change_type_supplying = 'delete'
    union
    select  _request_id, activity_id, name, tags.tag_ids_activity_snapshot(ca.activity_id, psa.snapshot_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'delete'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = _request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_receiving = 'delete'
    union
    select  _request_id, activity_id, name, tags.tag_ids_activity_directive(ca.activity_id, ad.plan_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'none'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = _request_id
        and plan_id = plan_id_R
        and ca.change_type_receiving = 'modify'
    union
    select  _request_id, activity_id, name, tags.tag_ids_activity_snapshot(ca.activity_id, psa.snapshot_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'modify'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = _request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_supplying = 'modify';

  -- Unlock so that updates can be written
  update merlin.plan
  set is_locked = false
  where id = plan_id_R;

  -- Update the plan's activities to match merge-staging-area's activities
  -- Add
  insert into merlin.activity_directive(
                id, plan_id, name, source_scheduling_goal_id, created_at, created_by, last_modified_by,
                start_offset, type, arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, name, source_scheduling_goal_id, created_at, created_by, last_modified_by,
            start_offset, type, arguments, metadata, anchor_id, anchored_to_start
   from merlin.merge_staging_area
  where merge_staging_area.merge_request_id = _request_id
    and change_type = 'add';

  -- Modify
  insert into merlin.activity_directive(
    id, plan_id, "name", source_scheduling_goal_id, created_at, created_by, last_modified_by,
    start_offset, "type", arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, "name", source_scheduling_goal_id, created_at, created_by, last_modified_by,
          start_offset, "type", arguments, metadata, anchor_id, anchored_to_start
  from merlin.merge_staging_area
  where merge_staging_area.merge_request_id = _request_id
    and change_type = 'modify'
  on conflict (id, plan_id)
  do update
  set name = excluded.name,
      source_scheduling_goal_id = excluded.source_scheduling_goal_id,
      created_at = excluded.created_at,
      created_by = excluded.created_by,
      last_modified_by = excluded.last_modified_by,
      start_offset = excluded.start_offset,
      type = excluded.type,
      arguments = excluded.arguments,
      metadata = excluded.metadata,
      anchor_id = excluded.anchor_id,
      anchored_to_start = excluded.anchored_to_start;

  -- Tags
  delete from tags.activity_directive_tags adt
    using merlin.merge_staging_area msa
    where adt.directive_id = msa.activity_id
      and adt.plan_id = plan_id_R
      and msa.merge_request_id = _request_id
      and msa.change_type = 'modify';

  insert into tags.activity_directive_tags(plan_id, directive_id, tag_id)
    select plan_id_R, activity_id, t.id
    from merlin.merge_staging_area msa
    inner join tags.tags t -- Inner join because it's specifically inserting into a tags-association table, so if there are no valid tags we do not want a null value for t.id
    on t.id = any(msa.tags)
    where msa.merge_request_id = _request_id
      and (change_type = 'modify'
       or change_type = 'add')
    on conflict (directive_id, plan_id, tag_id) do nothing;
  -- Presets
  insert into merlin.preset_to_directive(preset_id, activity_id, plan_id)
  select pts.preset_id, pts.activity_id, plan_id_R
  from merlin.merge_staging_area msa
  inner join merlin.preset_to_snapshot_directive pts using (activity_id)
  where pts.snapshot_id = snapshot_id_S
    and msa.merge_request_id = _request_id
    and (msa.change_type = 'add'
     or msa.change_type = 'modify')
  on conflict (activity_id, plan_id)
    do update
    set preset_id = excluded.preset_id;

  -- Delete
  delete from merlin.activity_directive ad
  using merlin.merge_staging_area msa
  where ad.id = msa.activity_id
    and ad.plan_id = plan_id_R
    and msa.merge_request_id = _request_id
    and msa.change_type = 'delete';

  -- Clean up
  delete from merlin.conflicting_activities where merge_request_id = _request_id;
  delete from merlin.merge_staging_area where merge_staging_area.merge_request_id = _request_id;

  update merlin.merge_request
  set status = 'accepted'
  where id = _request_id;

  -- Attach snapshot history
  insert into merlin.plan_latest_snapshot(plan_id, snapshot_id)
  select plan_id_receiving_changes, snapshot_id_supplying_changes
  from merlin.merge_request
  where id = _request_id;
end
$$;

create function merlin.duplicate_plan(_plan_id integer, new_plan_name text, new_owner text)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
  declare
    validate_plan_id integer;
    new_plan_id integer;
    created_snapshot_id integer;
begin
  select id from merlin.plan where plan.id = _plan_id into validate_plan_id;
  if(validate_plan_id is null) then
    raise exception 'Plan % does not exist.', _plan_id;
  end if;

  select merlin.create_snapshot(_plan_id) into created_snapshot_id;

  insert into merlin.plan(revision, name, model_id, duration, start_time, parent_id, owner, updated_by)
    select
        0, new_plan_name, model_id, duration, start_time, _plan_id, new_owner, new_owner
    from merlin.plan where id = _plan_id
    returning id into new_plan_id;
  insert into merlin.activity_directive(
      id, plan_id, name, source_scheduling_goal_id, created_at, created_by, last_modified_at, last_modified_by, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      id, new_plan_id, name, source_scheduling_goal_id, created_at, created_by, last_modified_at, last_modified_by, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from merlin.activity_directive where activity_directive.plan_id = _plan_id;

  with source_plan as (
    select simulation_template_id, arguments, simulation_start_time, simulation_end_time
    from merlin.simulation
    where simulation.plan_id = _plan_id
  )
  update merlin.simulation s
  set simulation_template_id = source_plan.simulation_template_id,
      arguments = source_plan.arguments,
      simulation_start_time = source_plan.simulation_start_time,
      simulation_end_time = source_plan.simulation_end_time
  from source_plan
  where s.plan_id = new_plan_id;

  insert into merlin.preset_to_directive(preset_id, activity_id, plan_id)
    select preset_id, activity_id, new_plan_id
    from merlin.preset_to_directive ptd where ptd.plan_id = _plan_id;

  insert into tags.plan_tags(plan_id, tag_id)
    select new_plan_id, tag_id
    from tags.plan_tags pt where pt.plan_id = _plan_id;
  insert into tags.activity_directive_tags(plan_id, directive_id, tag_id)
    select new_plan_id, directive_id, tag_id
    from tags.activity_directive_tags adt where adt.plan_id = _plan_id;

  insert into merlin.plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end
$$;

comment on function merlin.duplicate_plan(plan_id integer, new_plan_name text, new_owner text) is e''
  'Copies all of a given plan''s properties and activities into a new plan with the specified name.
  When duplicating a plan, a snapshot is created of the original plan.
  Additionally, that snapshot becomes the latest snapshot of the new plan.';

call migrations.mark_migration_rolled_back('11');
