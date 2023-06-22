-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  -- Schemas
  \ir schemas.sql
  -- Schema migrations
  \ir tables/schema_migrations.sql
  \ir applied_migrations.sql

  -- Scheduling intents.
  \ir tables/scheduling_goal.sql
  \ir tables/scheduling_specification.sql
  \ir tables/scheduling_specification_goals.sql
  \ir tables/scheduling_request.sql
  \ir tables/scheduling_goal_analysis.sql
  \ir tables/scheduling_goal_analysis_created_activities.sql
  \ir tables/scheduling_goal_analysis_satisfying_activities.sql
  \ir tables/scheduling_condition.sql
  \ir tables/scheduling_specification_conditions.sql

  -- Table-specific Metadata
  \ir tables/metadata/scheduling_goal_tags.sql
end;
