-- Add a trigger for creating simulation on plan insert
create function create_simulation_row_for_new_plan()
returns trigger
security definer
language plpgsql as $$begin
  insert into simulation (revision, simulation_template_id, plan_id, arguments)
  values (0, null, new.id, '{}');
  return new;
end
$$;

create trigger simulation_row_for_new_plan_trigger
after insert on plan
for each row
execute function create_simulation_row_for_new_plan();

-- Allow only one simulation row per plan
-- If your migration is failing, this is probably why.
alter table simulation add constraint one_simulation_per_plan unique(plan_id);

-- Modify Duplicate Plan to update rather than insert
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
end;
$$;

call migrations.mark_migration_applied('6');
