-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  -- Non-Public Schemas
  \ir schemas.sql

  -- Schema migrations
  \ir tables/migrations/schema_migrations.sql
  \ir applied_migrations.sql

  -- Domain types.
  \ir domain-types/merlin-arguments.sql
  \ir domain-types/merlin-activity-directive-metadata.sql
  \ir domain-types/plan-merge-types.sql

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
  \ir tables/plan_snapshot.sql
  \ir tables/plan_latest_snapshot.sql
  \ir tables/plan_snapshot_parent.sql
  \ir tables/plan_snapshot_activities.sql
  \ir tables/merge_request.sql
  \ir tables/merge_comments.sql
  \ir tables/merge_staging_area.sql
  \ir tables/conflicting_activities.sql
  \ir functions/public/duplicate_plan.sql
  \ir functions/public/plan_history_functions.sql
  \ir functions/public/get_merge_base.sql
  \ir functions/public/merge_request_state_functions.sql
  \ir functions/metadata/get_tag_ids.sql
  \ir functions/public/begin_merge.sql
  \ir functions/public/commit_merge.sql
  \ir functions/public/create_snapshot.sql

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

  -- Hasura Functions
  \ir functions/hasura/activity_preset_functions.sql
  \ir functions/hasura/delete_anchor_functions.sql
  \ir functions/hasura/hasura_functions.sql
  \ir functions/hasura/plan_branching_functions.sql
  \ir functions/hasura/plan_merge_functions.sql

end;
