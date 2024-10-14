create table merlin.external_source_type (
    name text not null,
    metadata merlin.argument_set,
    required_metadata merlin.required_parameter_set,

    constraint external_source_type_pkey
      primary key (name)
);

create or replace function merlin.validate_required_metadata_exist()
  returns trigger
  language plpgsql as $$
declare
  valid_metadata text[];
  required_metadata_names text[];
begin
  valid_metadata := (select array(select jsonb_object_keys(new.metadata)));
  required_metadata_names := (select array(select jsonb_array_elements_text(new.required_metadata)));
  if not (required_metadata_names <@ valid_metadata) then
    raise exception 'External source type definition contained required metadata that are not defined in the metadata of the type.';
  end if;
  return null;
end;
$$;

comment on table merlin.external_source_type is e''
  'Externally imported event source types (each external source has to be of a certain type).\n'
  'They are also helpful to classify external sources.\n'
  'Derivation groups are a subclass of external source type.';

comment on column merlin.external_source_type.name is e''
  'The identifier for this external_source_type, as well as its name.';
comment on column merlin.external_source_type.metadata is e''
  'All metadata, required or optional, that are tied ot each external source of this type.';
comment on column merlin.external_source_type.required_metadata is e''
  'A description of which metadata are required to be provided to instantiate external sources of this type.';
comment on function merlin.validate_required_metadata_exist() is e''
  'Validate that the metadata names in required_metadata actually exist within those defined in the metadata column.';

create trigger validate_required_metadata_exist
after insert on merlin.external_source_type
  for each row execute function merlin.validate_required_metadata_exist();

comment on trigger validate_required_metadata_exist on merlin.external_source_type is e''
  'Fires any time a new external source type is added that checks the required metadata of the external source type are also included in the metadata.';
