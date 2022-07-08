-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  -- Domain types.
  \ir domain-types/merlin-arguments.sql

  -- Tables.
  -- Uploaded files (JARs or simulation input files).
  \ir tables/uploaded_file.sql

  -- Planning intents.
  \ir tables/mission_model.sql
  \ir tables/activity_type.sql
  \ir tables/plan.sql
  \ir tables/activity.sql
  \ir tables/simulation_template.sql
  \ir tables/simulation.sql

  -- Uploaded datasets (or datasets generated from simulation).
  \ir tables/dataset.sql
  \ir tables/span.sql
  \ir tables/profile.sql
  \ir tables/profile_segment.sql
  \ir tables/topic.sql
  \ir tables/event.sql

  -- Analysis intents
  \ir tables/condition.sql
  \ir tables/profile_request.sql

  \ir tables/mission_model_parameters.sql
  \ir tables/simulation_dataset.sql
  \ir tables/plan_dataset.sql

  -- Views
  \ir views/simulated_activity.sql
  \ir views/resource_profile.sql
end;
