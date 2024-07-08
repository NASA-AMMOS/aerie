begin;

drop trigger if exists "notify_hasura_refreshSchedulingProcedureParameterTypes_INSERT" on scheduler.scheduling_goal_definition;
drop trigger if exists "notify_hasura_refreshSchedulingProcedureParameterTypes_UPDATE" on scheduler.scheduling_goal_definition;

alter table scheduler.scheduling_goal_analysis_created_activities
  drop constraint created_activities_primary_key,
  add constraint created_activities_primary_key
    primary key (analysis_id, goal_id, goal_revision, activity_id),

  drop column goal_invocation_id;

alter table scheduler.scheduling_goal_analysis
  drop constraint scheduling_goal_analysis_primary_key,
  add constraint scheduling_goal_analysis_primary_key
    primary key (analysis_id, goal_id, goal_revision),

  drop column goal_invocation_id,
  drop column arguments;

alter table scheduler.scheduling_specification_goals
  drop constraint scheduling_specification_goals_primary_key,
  add constraint scheduling_specification_goals_primary_key
    primary key (specification_id, goal_id),

  drop column goal_invocation_id,
  drop column arguments;

delete from scheduler.scheduling_goal_definition
  where definition is null;

alter table scheduler.scheduling_goal_definition
  drop constraint scheduling_procedure_has_uploaded_jar,

  drop column type,
  drop column uploaded_jar_id,
  drop column parameter_schema,

  alter column definition set not null;


drop type scheduler.goal_type;

call migrations.mark_migration_rolled_back('8');

commit;
