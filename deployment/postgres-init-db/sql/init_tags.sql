/*
  The order of inclusion is important!
    - Types must be loaded before usage in tables or function returns
    - Tables must be loaded before being referenced by foreign keys.
    - Functions must be loaded before they're used in triggers, but can be loaded after any functions that call them.
    - Views must be loaded after all their dependent tables and functions
 */
begin;
  -- Tables (Tags table must be created before the rest of the tags schema due to a reference in the Merlin schema)
  -- Merlin-associated tags
  \ir tables/tags/merlin/activity_directive_tags.sql
  \ir tables/tags/merlin/constraint_tags.sql
  \ir tables/tags/merlin/constraint_definition_tags.sql
  \ir tables/tags/merlin/plan_tags.sql
  \ir tables/tags/merlin/plan_snapshot_tags.sql
  \ir tables/tags/merlin/snapshot_activity_tags.sql

  -- Scheduler-associated tags
  \ir tables/tags/scheduling/scheduling_goal_tags.sql
  \ir tables/tags/scheduling/scheduling_goal_definition_tags.sql
  \ir tables/tags/scheduling/scheduling_condition_tags.sql
  \ir tables/tags/scheduling/scheduling_condition_definition_tags.sql

  -- Sequencing-associated tags
  \ir tables/tags/expansion_rule_tags.sql

  -- Functions
  \ir functions/tags/get_tag_ids.sql
end;
