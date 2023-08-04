alter table activity_type
  rename column parameters to parameter_definitions;
alter table activity_type
  rename column computed_attribute_value_schema to computed_attribute_definitions;

alter table resource_type
  rename column schema to definition;
comment on column resource_type.definition is e''
  'The definition including the structure of this resource type.';

alter table mission_model_parameters
  rename column parameters to parameter_definitions;

call migrations.mark_migration_applied('23');
