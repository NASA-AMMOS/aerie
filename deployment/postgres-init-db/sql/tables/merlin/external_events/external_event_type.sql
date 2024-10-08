create table merlin.external_event_type (
    name text not null,
    properties merlin.argument_set,
    required_properties merlin.required_parameter_set,

    constraint external_event_type_pkey
      primary key (name)
);

-- Add a trigger to validate that the properties in 'required_properties' actually exist in 'properties'. TODO this can probably be simplified from a trigger but I had issues doing a 'check' constraint b/c of casting the compared columns to text[]s
create or replace function merlin.validate_required_properties_exist()
  returns trigger
  language plpgsql as
$func$
declare
  valid_properties text[];
  required_property_names text[];
begin
  valid_properties := (select array(select jsonb_object_keys(new.properties)));
  required_property_names := (select array(select jsonb_array_elements_text(new.required_properties)));
  if not (required_property_names <@ valid_properties) then
    raise exception 'External event type definition contained required properties that are not defined in the properties of the type.';
  end if;
  return null;
end;
$func$;

comment on table merlin.external_event_type is e''
  'Externally imported event types.';

comment on column merlin.external_event_type.name is e''
  'The identifier for this external_event_type, as well as its name.';
comment on column merlin.external_event_type.properties is e''
  'All properties, required or optional, that are tied to each external event of this type';
comment on column merlin.external_event_type.required_properties is e''
  'A description of which properties are required to be provided to instantiate external events of this type';

comment on function merlin.validate_required_properties_exist() is e''
  'Validate that the property names in required_properties actually exist within those defined in the properties column.';

create trigger validate_required_properties_exist
after insert on merlin.external_event_type
  for each row execute function merlin.validate_required_properties_exist();

comment on trigger validate_required_properties_exist on merlin.external_event_type is e''
  'Fires any time a new external event type is added that checks the required properties of the external event type are also included in the properties.';
