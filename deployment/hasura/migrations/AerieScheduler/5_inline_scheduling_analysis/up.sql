-- Drop Triggers
drop trigger create_scheduling_analysis_trigger on scheduling_request;
drop function create_scheduling_analysis();

-- Update FK relationships
alter table scheduling_request
  alter column analysis_id add generated always as identity,
  drop constraint scheduling_request_references_analysis;

alter table scheduling_goal_analysis_satisfying_activities
  drop constraint satisfying_activities_references_scheduling_analysis,
  add constraint satisfying_activities_references_scheduling_request
    foreign key (analysis_id)
      references scheduling_request (analysis_id)
      on update cascade
      on delete cascade;

alter table scheduling_goal_analysis
  drop constraint scheduling_goal_analysis_references_scheduling_analysis,
  add constraint scheduling_goal_analysis_references_scheduling_request
    foreign key (analysis_id)
      references scheduling_request (analysis_id)
      on update cascade
      on delete cascade;

alter table scheduling_goal_analysis_created_activities
  drop constraint created_activities_references_scheduling_analysis,
  add constraint created_activities_references_scheduling_request
    foreign key (analysis_id)
      references scheduling_request (analysis_id)
      on update cascade
      on delete cascade;

-- Drop Scheduling Analysis
comment on column scheduling_analysis.id is null;
comment on table scheduling_analysis is null;
drop table scheduling_analysis;

call migrations.mark_migration_applied('5');
