-- Ensure the update trigger has the correct "when" condition
create or replace trigger update_scheduling_specification_goal
  before update on scheduler.scheduling_specification_goals
  for each row
  when (OLD.priority is distinct from NEW.priority and pg_trigger_depth() < 1)
execute function scheduler.update_scheduling_specification_goal_func();

call migrations.mark_migration_applied('2');
