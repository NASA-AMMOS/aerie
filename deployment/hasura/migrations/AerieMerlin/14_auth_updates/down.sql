-- Simulation Template
comment on column simulation_template.owner is null;
alter table simulation_template
  drop column owner;

-- Activity Preset
comment on column activity_presets.owner is null;
alter table activity_preset
  drop column owner;

call migrations.mark_migration_rolled_back('14');
