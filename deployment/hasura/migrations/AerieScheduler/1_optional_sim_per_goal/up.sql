alter table scheduling_specification_goals add column simulate_after boolean not null default true;

comment on column scheduling_specification_goals.simulate_after is e''
  'Whether to re-simulate after evaluating this goal and before the next goal.';

call migrations.mark_migration_applied('1');
