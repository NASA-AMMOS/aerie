-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;

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

end;
