-- The order of inclusion is important! Tables referenced by foreign keys must be loaded before their dependants.

begin;
  -- Scheduling intents.
  \ir tables/scheduling_rule.sql
end;
