-- Create table for external events
create table merlin.external_event (
    key text not null,
    event_type_name text not null,
	  source_key text not null,
	  source_type_name text not null,
    derivation_group_name text not null,
    start_time timestamp with time zone not null,
    duration interval not null,
    properties jsonb,

    constraint external_event_pkey
      primary key (key, source_key, derivation_group_name, event_type_name),
    constraint external_event_references_source_id
      foreign key (source_key, derivation_group_name)
      references merlin.external_source (key, derivation_group_name),
    constraint external_event_references_event_type_name
      foreign key (event_type_name)
      references merlin.external_event_type(name)
);

comment on table merlin.external_event is e''
  'A table for externally imported events.';
