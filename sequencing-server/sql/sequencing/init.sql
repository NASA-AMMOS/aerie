-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  --Schemas
  \ir schemas.sql

  -- Schema migrations
  \ir tables/schema_migrations.sql
  \ir applied_migrations.sql

  -- Command Expansion Tables.
  \ir tables/command_dictionary.sql
  \ir tables/expansion_set.sql
  \ir tables/expansion_rule.sql
  \ir tables/expansion_set_to_rule.sql
  \ir tables/expansion_run.sql
  \ir tables/activity_instance_commands.sql
  \ir tables/sequence.sql
  \ir tables/sequence_to_simulated_activity.sql
  \ir tables/user_sequence.sql
  \ir tables/expanded_sequences.sql

  -- Table-specific Metadata
  \ir tables/metadata/expansion_rule_tags.sql
end;
