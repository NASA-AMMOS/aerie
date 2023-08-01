alter table activity_type
  add column parameter_units jsonb not null default '{}',
  add column computed_attribute_units jsonb not null default '{}';
comment on column activity_type.parameter_units is e''
  'The parameter units optionally defined in the mission model.';
comment on column activity_type.computed_attribute_units is e''
  'The computed attribute units optionally defined in the mission model.';

alter table resource_type
  add column units jsonb not null default '{}';
comment on column resource_type.units is e''
  'The optionally defined units for the resource type';

call migrations.mark_migration_applied('22');
