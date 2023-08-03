comment on column activity_type.parameter_units is null;
comment on column activity_type.computed_attribute_units is null;
alter table activity_type
  drop column parameter_units,
  drop column computed_attribute_units;

comment on column resource_type.units is null;
alter table resource_type
  drop column units;

call migrations.mark_migration_rolled_back('22');
