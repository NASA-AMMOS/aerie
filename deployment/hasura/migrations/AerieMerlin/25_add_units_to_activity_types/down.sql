alter table activity_type
  rename column parameter_definitions to parameters;
alter table activity_type
  rename column computed_attribute_definitions to computed_attribute_value_schema;

alter table resource_type
  rename column definition to schema;
comment on column resource_type.definition is e''
  'The structure of this resource type.';

alter table mission_model_parameters
  rename column parameter_definitions to parameters;

call migrations.mark_migration_rolled_back('25');
