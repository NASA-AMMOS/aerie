-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  -- Schema migrations
  \ir tables/schema_migrations.sql
  \ir applied_migrations.sql

  -- Domain types.
  \ir domain-types/merlin-arguments.sql
  \ir domain-types/merlin-activity-directive-metadata.sql

  -- Deployment-level Metadata
  \ir tables/metadata/tags.sql

  -- Activity Directive Metadata schema
  \ir tables/activity_directive_metadata_schema.sql

  -- Tables.
  -- Uploaded files (JARs or simulation input files).
  \ir tables/uploaded_file.sql

  -- Planning intents.
  \ir tables/mission_model.sql
  \ir tables/activity_type.sql
  \ir tables/resource_type.sql
  \ir tables/plan.sql
  \ir tables/plan_collaborators.sql
  \ir tables/activity_directive.sql
  \ir tables/activity_directive_validations.sql
  \ir tables/anchor_validation_status.sql
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
  \ir tables/constraint.sql

  \ir tables/mission_model_parameters.sql
  \ir tables/simulation_dataset.sql
  \ir tables/plan_dataset.sql

  -- Plan Collaboration
  \ir plan_collaboration.sql
  \ir merge_request.sql
  \ir merge_comments.sql
  \ir plan_merge.sql
  \ir hasura_functions.sql

  -- Presets
  \ir tables/activity_presets.sql

  -- Table-specific Metadata
  \ir tables/metadata/activity_directive_tags.sql
  \ir tables/metadata/constraint_tags.sql
  \ir tables/metadata/plan_tags.sql
  \ir tables/metadata/snapshot_activity_tags.sql

  -- Views
  \ir views/simulated_activity.sql
  \ir views/resource_profile.sql
  \ir views/activity_directive_extended.sql
end;
