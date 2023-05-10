-- Activity Preset
alter table activity_preset
  add column owner text not null default '';
comment on column activity_presets.owner is e''
  'The owner of this activity preset';

-- Simulation Template
alter table simulation_template
  add column owner text not null default '';
comment on column simulation_template.owner is e''
  'The user responsible for this simulation template';

call migrations.mark_migration_applied('14');
