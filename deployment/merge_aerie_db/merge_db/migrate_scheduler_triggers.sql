begin;
---------------------
-- UPDATE TRIGGERS --
---------------------
create or replace trigger increment_revision_on_update_trigger
  before update on scheduler.scheduling_specification
  for each row
  when (pg_trigger_depth() < 1)
  execute function util_functions.increment_revision_update();
drop function scheduler.increment_revision_on_update();

create or replace trigger set_timestamp
before update on scheduler.scheduling_condition_metadata
for each row
execute function util_functions.set_updated_at();
drop function scheduler.scheduling_condition_metadata_set_updated_at();

create or replace trigger set_timestamp
before update on scheduler.scheduling_goal_metadata
for each row
execute function util_functions.set_updated_at();
drop function scheduler.scheduling_goal_metadata_set_updated_at();

create function scheduler.create_scheduling_spec_for_new_plan()
returns trigger
security definer
language plpgsql as $$
declare
  spec_id integer;
begin
  -- Create a new scheduling specification
  insert into scheduler.scheduling_specification (revision, plan_id, plan_revision, horizon_start, horizon_end,
                                                  simulation_arguments, analysis_only)
  values (0, new.id, new.revision, new.start_time, new.start_time+new.duration, '{}', false)
  returning id into spec_id;

  -- Populate the scheduling specification
  insert into scheduler.scheduling_specification_goals (specification_id, goal_id, goal_revision, priority)
  select spec_id, msg.goal_id, msg.goal_revision, msg.priority
  from scheduler.scheduling_model_specification_goals msg
  where msg.model_id = new.model_id
  order by msg.priority;

  insert into scheduler.scheduling_specification_conditions (specification_id, condition_id, condition_revision)
  select spec_id, msc.condition_id, msc.condition_revision
  from scheduler.scheduling_model_specification_conditions msc
  where msc.model_id = new.model_id;

  return new;
end
$$;

comment on function scheduler.create_scheduling_spec_for_new_plan() is e''
'Creates a scheduling specification for a new plan
 and populates it with the contents of the plan''s model''s specification.';

create trigger scheduling_spec_for_new_plan_trigger
after insert on merlin.plan
for each row
execute function scheduler.create_scheduling_spec_for_new_plan();
end;
