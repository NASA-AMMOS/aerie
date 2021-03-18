-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.
begin;
  -- Domain types.
  --
    \ir domain-types/merlin-arguments.sql
  --

  -- Input from users.
  --
    -- Uploaded files (JARs or simulation input files).
    \ir tables/uploaded_file.sql

    -- Planning intents.
    \ir tables/mission_model.sql
    \ir tables/plan.sql
    \ir tables/activity.sql
    \ir tables/simulation.sql

    -- Uploaded datasets (or datasets generated from simulation).
    \ir tables/dataset.sql
    \ir tables/span.sql
    \ir tables/profile.sql
    \ir tables/profile_segment.sql

    -- Analysis intents
    \ir tables/condition.sql
    \ir tables/profile_request.sql
  --

  -- TODO: Comment everything.

  -- Derived tables and process state.
  --
    -- Derived by processing an uploaded mission model.
    \ir tables/mission_model_parameters.sql
    \ir tables/activity_type.sql

    -- Derived by simulating a plan.
    \ir tables/simulation_dataset.sql
  --
end;
