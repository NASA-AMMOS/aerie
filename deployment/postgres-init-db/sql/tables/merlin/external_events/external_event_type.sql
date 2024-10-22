create table merlin.external_event_type (
    name text not null,
    metadata merlin.argument_set,
    required_metadata merlin.required_parameter_set,

    constraint external_event_type_pkey
      primary key (name)
);

-- Add a trigger to validate that the metadata in 'required_metadata' actually exist in 'metadata'. TODO this can probably be simplified from a trigger but I had issues doing a 'check' constraint b/c of casting the compared columns to text[]s
create or replace function merlin.validate_required_metadata_exist()
  returns trigger
  language plpgsql as
$func$
declare
  valid_metadata text[];
  required_metadata_names text[];
begin
  valid_metadata := (select array(select jsonb_object_keys(new.metadata)));
  required_metadata_names := (select array(select jsonb_array_elements_text(new.required_metadata)));
  if not (required_metadata_names <@ valid_metadata) then
    raise exception 'External event type definition contained required metadata that are not defined in the metadata of the type.';
  end if;
  return null;
end;
$func$;

comment on table merlin.external_event_type is e''
  'Externally imported event types.';

comment on column merlin.external_event_type.name is e''
  'The identifier for this external_event_type, as well as its name.';
comment on column merlin.external_event_type.metadata is e''
  'All metadata, required or optional, that are tied to each external event of this type';
comment on column merlin.external_event_type.required_metadata is e''
  'A description of which metadata are required to be provided to instantiate external events of this type';

comment on function merlin.validate_required_metadata_exist() is e''
  'Validate that the metadata names in required_metadata actually exist within those defined in the metadata column.';

create trigger validate_required_metadata_exist
after insert on merlin.external_event_type
  for each row execute function merlin.validate_required_metadata_exist();

comment on trigger validate_required_metadata_exist on merlin.external_event_type is e''
  'Fires any time a new external event type is added that checks the required metadata of the external event type are also included in the metadata.';
