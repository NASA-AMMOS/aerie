alter table scheduling_specification_goals add constraint scheduling_specification_unique_goal_id unique (goal_id);
alter table scheduling_specification add constraint scheduling_specification_unique_plan_id unique (plan_id);

call migrations.mark_migration_applied('2');
