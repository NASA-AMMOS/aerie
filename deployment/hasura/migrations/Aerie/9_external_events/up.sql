-- Create table for external source types
create table merlin.external_source_type (
    name text not null,

    constraint external_source_type_pkey
      primary key (name)
);

comment on table merlin.external_source_type is e''
  'A table for externally imported event source types.';

-- Create table for external event types
create table merlin.external_event_type (
    name text not null,

    constraint external_event_type_pkey
      primary key (name)
);

comment on table merlin.external_event_type is e''
  'A table for externally imported event types.';

-- Create a table to represent derivation groups for external sources
create table merlin.derivation_group (
    name text not null unique,
    source_type_name text not null,

    constraint derivation_group_pkey
      primary key (name, source_type_name),
    constraint derivation_group_references_external_source_type
      foreign key (source_type_name)
      references merlin.external_source_type(name)
);
comment on table merlin.derivation_group is e''
  'A table to represent the names of groups of sources to run derivation operations over.';

-- Create a table to represent external event sources.
create table merlin.external_source (
    key text not null,
    source_type_name text not null,
    derivation_group_name text not null,
    valid_at timestamp with time zone not null,
    start_time timestamp with time zone not null,
    end_time timestamp with time zone not null,
    CHECK (end_time > start_time),
    created_at timestamp with time zone default now() not null,
    metadata jsonb,

    constraint external_source_pkey
      primary key (key, derivation_group_name),
    -- a given dg cannot have two sources with the same valid_at!
    CONSTRAINT dg_unique_valid_at UNIQUE (derivation_group_name, valid_at),
    constraint external_source_references_external_source_type_name
      foreign key (source_type_name)
      references merlin.external_source_type(name),
    constraint external_source_type_matches_derivation_group
      foreign key (derivation_group_name, source_type_name)
      references merlin.derivation_group (name, source_type_name)
);

comment on table merlin.external_source is e''
  'A table for externally imported event sources.';

-- Create table for external events
create table merlin.external_event (
    key text not null,
    event_type_name text not null,
	  source_key text not null,
    derivation_group_name text not null,
    start_time timestamp with time zone not null,
    duration interval not null,
    properties jsonb,

    constraint external_event_pkey
      primary key (key, source_key, derivation_group_name, event_type_name),
    constraint external_event_references_source_key_derivation_group
      foreign key (source_key, derivation_group_name)
      references merlin.external_source (key, derivation_group_name),
    constraint external_event_references_event_type_name
      foreign key (event_type_name)
      references merlin.external_event_type(name)
);

comment on table merlin.external_event is e''
  'A table for externally imported events.';

-- Create table for plan/external event links
create table merlin.plan_derivation_group (
    plan_id integer not null,
    derivation_group_name text not null,
    enabled boolean not null default true,
    created_at timestamp with time zone default now() not null,

    constraint plan_derivation_group_pkey
      primary key (plan_id, derivation_group_name),
    constraint plan_derivation_group_references_plan_id
      foreign key (plan_id)
      references merlin.plan(id),
    constraint plan_derivation_group_references_derivation_group_name
      foreign key (derivation_group_name)
      references merlin.derivation_group(name)
);

comment on table merlin.plan_derivation_group is e''
  'A table for linking externally imported event sources & plans.';

-- Add a trigger verifying that events fit into their sources
create or replace function merlin.check_event_times()
 	returns trigger
 	language plpgsql as
$func$
declare
	source_start timestamp with time zone;
	source_end timestamp with time zone;
	event_start timestamp with time zone;
	event_end timestamp with time zone;
begin
  	select start_time into source_start from merlin.external_source where new.source_key = external_source.key and new.derivation_group_name = external_source.derivation_group_name;
  	select end_time into source_end from merlin.external_source where new.source_key = external_source.key AND new.derivation_group_name = external_source.derivation_group_name;
    event_start := new.start_time;
	event_end := new.start_time + new.duration;
	if event_start < source_start or event_end < source_start then
		raise exception 'Event %s out of bounds of source %s', new.key, new.source_key;
	end if;
	if event_start > source_end or event_end > source_end then
		raise exception 'Event %s out of bounds of source %s', new.key, new.source_key;
	end if;
	return null;
end;
$func$;

create trigger check_event_times
after insert on merlin.external_event
	for each row execute function merlin.check_event_times();

-- Create a table to track which sources the user has and has not seen added/removed
create table ui.seen_sources
(
    username text not null,
    external_source_name text not null,
    external_source_type text not null, -- included for ease of filtering, though not part of pkey
    derivation_group text not null,

    constraint seen_sources_pkey
      primary key (username, external_source_name, derivation_group),
    constraint seen_sources_references_user
      foreign key (username)
      references permissions.users (username) match simple
);

comment on table ui.seen_sources is e''
  'A table for tracking the external sources acknowledge/unacknowledged by each user.';

-- create a function to aid the derived_events view, around diffing time ranges
create function merlin.subtract_later_ranges(curr_date tstzmultirange, later_dates tstzmultirange[])
returns tstzmultirange
language plpgsql as $$
  declare
	  ret tstzmultirange := curr_date;
	  later_date tstzmultirange;
begin
	foreach later_date in array later_dates loop
		ret := ret - later_date;
	end loop;
	return ret;
end
$$;

-- create a view that derives events from different sources in a given derivation group
create or replace view merlin.derived_events
as
select source_key,
  derivation_group_name,
  event_key,
  duration,
  event_type_name,
  start_time,
  source_range,
  valid_at
from ( select rule1_3.source_key,
        rule1_3.event_key,
        rule1_3.event_type_name,
        rule1_3.duration,
        rule1_3.derivation_group_name,
        rule1_3.start_time,
        rule1_3.source_range,
        rule1_3.valid_at,
        row_number() over (partition by rule1_3.event_key, rule1_3.derivation_group_name order by rule1_3.valid_at desc) as rn
        from ( select sub.key as source_key,
                external_event.key as event_key,
                external_event.event_type_name,
                external_event.duration,
                sub.derivation_group_name,
                external_event.start_time,
                sub.source_range,
                sub.valid_at
                from merlin.external_event
                join ( with derivation_tb_range as (
                        select external_source.key,
                                external_source.derivation_group_name,
                                tstzmultirange(tstzrange(external_source.start_time, external_source.end_time)) AS dr,
                                external_source.valid_at
                                from merlin.external_source
                                order by external_source.valid_at
                        ), ranges_with_subs as (
                        select tr1.key,
                              tr1.derivation_group_name,
                              tr1.dr as original_range,
                              coalesce(array_remove(array_agg(tr2.dr order by tr2.valid_at) FILTER (where tr1.derivation_group_name = tr2.derivation_group_name), null::tstzmultirange), '{}'::tstzmultirange[]) as subsequent_ranges,
                              tr1.valid_at
                              from derivation_tb_range tr1
                                  left join derivation_tb_range tr2 on tr1.valid_at < tr2.valid_at
                              group by tr1.key, tr1.derivation_group_name, tr1.valid_at, tr1.dr
                        )
                        select ranges_with_subs.key,
                              ranges_with_subs.derivation_group_name,
                              ranges_with_subs.original_range,
                              ranges_with_subs.subsequent_ranges,
                              merlin.subtract_later_ranges(ranges_with_subs.original_range, ranges_with_subs.subsequent_ranges) AS source_range,
                              ranges_with_subs.valid_at
                        from ranges_with_subs
                        order by ranges_with_subs.derivation_group_name desc, ranges_with_subs.valid_at) sub on sub.key = external_event.source_key and sub.derivation_group_name = external_event.derivation_group_name
                where sub.source_range @> external_event.start_time
        order by sub.derivation_group_name, external_event.start_time) rule1_3) t
where rn = 1
order by start_time;

comment on view  merlin.derived_events is e''
  'A view detailing all derived events from the ';

-- create a view that aggregates additional derivation group information
create or replace view merlin.derivation_group_comp
  as
select
  name,
  source_type_name,
  array_agg(distinct concat(sources.source_key, ', ', sources.derivation_group_name, ', ', sources.contained_events)) as sources,
  array_remove(array_agg(distinct event_type_name), null) as event_types,
	count(distinct counted.event_key) as derived_total
from (
	select derivation_group.name,
		     derivation_group.source_type_name,
		     types.event_type_name
  from merlin.derivation_group
	left outer join ( select external_event.source_key,
	                         external_event.derivation_group_name,
	                         external_event.event_type_name
			              from merlin.external_event
			              order by external_event.source_key) types on types.derivation_group_name = derivation_group.name
	group by derivation_group.name, derivation_group.source_type_name, types.event_type_name
) with_event_types
full outer join ( select external_event.source_key,
                         external_event.derivation_group_name,
                         count(external_event.key) as contained_events
                  from merlin.external_event
                  group by external_event.source_key, external_event.derivation_group_name
                  order by external_event.source_key) sources on sources.derivation_group_name = with_event_types.name
full outer join ( select derived_events.event_key,
				                 derived_events.derivation_group_name
			            from merlin.derived_events) counted on counted.derivation_group_name = with_event_types.name
group by with_event_types.name, with_event_types.source_type_name;

comment on view  merlin.derivation_group_comp is e''
  'A view detailing all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. As such, a new view was created to lighten the load.';

-- update pre-existing functions now associated with branching:
-- duplicate_plan:
create or replace function merlin.duplicate_plan(_plan_id integer, new_plan_name text, new_owner text)
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

  insert into merlin.plan_derivation_group(derivation_group_name, plan_id)
    select derivation_group_name, new_plan_id
  	from merlin.plan_derivation_group pdg where pdg.plan_id = _plan_id;

  insert into merlin.plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end
$$;

comment on function merlin.duplicate_plan(plan_id integer, new_plan_name text, new_owner text) is e''
  'Copies all of a given plan''s properties and activities into a new plan with the specified name.
  When duplicating a plan, a snapshot is created of the original plan.
  Additionally, that snapshot becomes the latest snapshot of the new plan.';

-- commit_merge:
create or replace procedure merlin.commit_merge(_request_id integer)
  language plpgsql as $$
  declare
    validate_noConflicts integer;
    plan_id_R integer;
    plan_id_S integer;
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

  -- Perform an OR operation on the plans' derivation groups
  select plan_id from merlin.plan_snapshot ps where ps.snapshot_id = snapshot_id_S into plan_id_S;

  insert into merlin.plan_derivation_group(derivation_group_name, plan_id)
	  select derivation_group_name, plan_id_R
	  from merlin.plan_derivation_group pdg where pdg.plan_id = plan_id_S
  on conflict(derivation_group_name, plan_id) do nothing;
  -- as branch still exists, no need to delete anything or perform any cleanup in merlin.plan_derivation_group.

  -- Attach snapshot history
  insert into merlin.plan_latest_snapshot(plan_id, snapshot_id)
  select plan_id_receiving_changes, snapshot_id_supplying_changes
  from merlin.merge_request
  where id = _request_id;
end
$$;

call migrations.mark_migration_applied('9');
