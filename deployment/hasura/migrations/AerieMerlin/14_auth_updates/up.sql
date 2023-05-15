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

-- Profile Request
comment on column profile_request.duration is null;
comment on column profile_request.profile_name is null;
comment on column profile_request.dataset_id is null;
comment on table profile_request is null;

drop table profile_request;

call migrations.mark_migration_applied('14');
