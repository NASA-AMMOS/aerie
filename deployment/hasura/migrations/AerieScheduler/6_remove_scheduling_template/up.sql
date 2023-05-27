-- Scheduling Template Goals
comment on function delete_scheduling_template_goal_func() is null;
comment on function update_scheduling_template_goal_func is null;
comment on function insert_scheduling_template_goal_func is null;

comment on column scheduling_template_goals.priority is null;
comment on column scheduling_template_goals.goal_id is null;
comment on column scheduling_template_goals.template_id is null;
comment on table scheduling_template_goals is null;

drop trigger delete_scheduling_template_goal on scheduling_template_goals;
drop trigger update_scheduling_template_goal on scheduling_template_goals;
drop trigger insert_scheduling_template_goal on scheduling_template_goals;

drop function delete_scheduling_template_goal_func();
drop function update_scheduling_template_goal_func();
drop function insert_scheduling_template_goal_func();

drop table scheduling_template_goals;

-- Scheduling Template
comment on column scheduling_template.simulation_arguments is null;
comment on column scheduling_template.description is null;
comment on column scheduling_template.model_id is null;
comment on column scheduling_template.revision is null;
comment on column scheduling_template.id is null;
comment on table scheduling_template is null;

drop trigger increment_revision_on_update_scheduling_template_trigger on scheduling_template;
drop function increment_revision_on_update_scheduling_template();

drop table scheduling_template;

call migrations.mark_migration_applied('6');
