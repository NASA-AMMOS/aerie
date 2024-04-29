-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  -- Schemas
  \ir schemas.sql
  -- Schema migrations
  \ir tables/schema_migrations.sql
  \ir applied_migrations.sql

  -- Scheduling Goals
  \ir tables/scheduling_goal_metadata.sql
  \ir tables/scheduling_goal_definition.sql

  -- Scheduling Conditions
  \ir tables/scheduling_condition_metadata.sql
  \ir tables/scheduling_condition_definition.sql

  -- Scheduling Specification
  \ir tables/scheduling_specification.sql
  \ir tables/scheduling_specification_goals.sql
  \ir tables/scheduling_specification_conditions.sql
  \ir tables/scheduling_model_specification_conditions.sql
  \ir tables/scheduling_model_specification_goals.sql

  -- Scheduling Output
  \ir tables/scheduling_request.sql
  \ir tables/scheduling_goal_analysis.sql
  \ir tables/scheduling_goal_analysis_created_activities.sql
  \ir tables/scheduling_goal_analysis_satisfying_activities.sql

  -- Table-specific Metadata
  \ir tables/metadata/scheduling_goal_tags.sql
  \ir tables/metadata/scheduling_goal_definition_tags.sql
  \ir tables/metadata/scheduling_condition_tags.sql
  \ir tables/metadata/scheduling_condition_definition_tags.sql
end;
