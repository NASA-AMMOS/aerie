alter table simulation
  add column simulation_start_time timestamptz default null,
  add column simulation_end_time timestamptz default null;

alter table simulation_template
  add column simulation_start_time timestamptz default null,
  add column simulation_end_time timestamptz default null;

call migrations.mark_migration_applied('5');
