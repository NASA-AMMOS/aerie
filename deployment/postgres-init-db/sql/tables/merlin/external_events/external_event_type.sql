create table merlin.external_event_type (
    name text not null,
    properties merlin.parameter_set,
    required_properties merlin.required_parameter_set,
    computed_attributes_value_schema jsonb,
    constraint external_event_type_pkey
      primary key (name)
);

comment on table merlin.external_event_type is e''
  'Externally imported event types.';

comment on column merlin.external_event_type.name is e''
  'The identifier for this external_event_type, as well as its name.';
  'A table for externally imported event types.';
comment on column merlin.external_event_type.properties is e''
  'All properties, required or optional, that are tied to each external event of this type';
comment on column merlin.external_event_type.required_properties is e''
  'A description of which properties are required to be provided to instantiate external events of this type';
comment on column merlin.external_event_type.computed_attributes_value_schema is e''
'The type of value returned by the effect model of this external event type';
