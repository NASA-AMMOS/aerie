alter table activity_type
  add column units jsonb not null default '{}';
comment on column activity_type.units is e''
  'The units optionally defined in the mission model.';

alter table resource_type
  add column units jsonb not null default '{}';
comment on column resource_type.units is e''
  'The optionally defined units for the resource type';

call migrations.mark_migration_applied('22');
