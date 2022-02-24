-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  -- Scheduling intents.
  \ir tables/scheduling_goal.sql
  \ir tables/scheduling_template.sql
  \ir tables/scheduling_template_goals.sql
  \ir tables/scheduling_specification.sql
  \ir tables/scheduling_specification_goals.sql
  \ir tables/scheduling_analysis.sql
  \ir tables/scheduling_request.sql
  \ir tables/scheduling_goal_analysis.sql
  \ir tables/scheduling_goal_analysis_created_activities.sql
  \ir tables/scheduling_goal_analysis_satisfying_activities.sql

end;
