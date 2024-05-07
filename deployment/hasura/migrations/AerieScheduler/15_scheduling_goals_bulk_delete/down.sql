-- Restore Scheduling Spec (Model) Delete function to work per-row
create or replace trigger delete_scheduling_model_specification_goal
  after delete on scheduling_model_specification_goals
  for each row
execute function delete_scheduling_model_specification_goal_func();

create or replace function delete_scheduling_model_specification_goal_func()
  returns trigger
  language plpgsql as $$
begin
  update scheduling_model_specification_goals
  set priority = priority - 1
  where model_id = old.model_id
    and priority > old.priority;
  return null;
end;
$$;

-- Restore Scheduling Spec (Plan) Delete function to work per-row
create or replace trigger delete_scheduling_specification_goal
  after delete on scheduling_specification_goals
  for each row
execute function delete_scheduling_specification_goal_func();

create or replace function delete_scheduling_specification_goal_func()
  returns trigger
  language plpgsql as $$
begin
  update scheduling_specification_goals
  set priority = priority - 1
  where specification_id = old.specification_id
    and priority > old.priority;
  return null;
end;
$$;

-- Remove the "depth" condition from this trigger
create or replace trigger increment_revision_on_goal_update
  before insert or update on scheduling_specification_goals
  for each row
execute function increment_spec_revision_on_goal_spec_update();

call migrations.mark_migration_rolled_back('15');
