alter table simulation
  drop column simulation_start_time,
  drop column simulation_end_time;

alter table simulation_template
  drop column simulation_start_time,
  drop column simulation_end_time;

call migrations.mark_migration_rolled_back('5');
