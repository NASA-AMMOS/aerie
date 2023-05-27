-- Duplicate Plan
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

create function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json)
  returns hasura_functions.duplicate_plan_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
  new_owner text;
begin
  new_owner := (hasura_session ->> 'x-hasura-user-id');
  select duplicate_plan(plan_id, new_plan_name, new_owner) into res;
  return row(res)::hasura_functions.duplicate_plan_return_value;
end;
$$;

comment on function duplicate_plan(plan_id integer, new_plan_name text) is null;
comment on function duplicate_plan(plan_id integer, new_plan_name text, new_owner text) is e''
  'Copies all of a given plan''s properties and activities into a new plan with the specified name.
  When duplicating a plan, a snapshot is created of the original plan.
  Additionally, that snapshot becomes the latest snapshot of the new plan.';

drop function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text);
drop function public.duplicate_plan(plan_id integer, new_plan_name text);

-- Create Merge Request
create function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, hasura_session json)
  returns hasura_functions.create_merge_request_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
  requester_username text;
begin
  requester_username := (hasura_session ->> 'x-hasura-user-id');
  select create_merge_request(source_plan_id, target_plan_id, requester_username) into res;
  return row(res)::hasura_functions.create_merge_request_return_value;
end;
$$;

drop function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, requester_username text);

-- Begin Merge
create function hasura_functions.begin_merge(merge_request_id integer, hasura_session json)
  returns hasura_functions.begin_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
  declare
    non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[];
    conflicting_activities hasura_functions.get_conflicting_activities_return_value[];
    reviewer_username text;
begin
  reviewer_username := (hasura_session ->> 'x-hasura-user-id');
  call public.begin_merge($1, reviewer_username);

  non_conflicting_activities := array(select hasura_functions.get_non_conflicting_activities($1));
  conflicting_activities := array(select hasura_functions.get_conflicting_activities($1));

  return row($1, non_conflicting_activities, conflicting_activities)::hasura_functions.begin_merge_return_value;
end;
$$;

drop function hasura_functions.begin_merge(merge_request_id integer, reviewer_username text);


-- Activity Preset
alter table public.activity_presets
  add column owner text not null default '';
comment on column activity_presets.owner is e''
  'The owner of this activity preset';

-- Simulation Template
alter table public.simulation_template
  add column owner text not null default '';
comment on column simulation_template.owner is e''
  'The user responsible for this simulation template';

-- Profile Request
comment on column profile_request.duration is null;
comment on column profile_request.profile_name is null;
comment on column profile_request.dataset_id is null;
comment on table profile_request is null;

drop table public.profile_request;

call migrations.mark_migration_applied('14');
