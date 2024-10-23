create table merlin.external_event (
    key text not null,
    event_type_name text not null,
    source_key text not null,
    derivation_group_name text not null,
    start_time timestamp with time zone not null,
    duration interval not null,
    metadata merlin.argument_set,

    constraint external_event_pkey
      primary key (key, source_key, derivation_group_name, event_type_name),
    constraint external_event_references_source_key_derivation_group
      foreign key (source_key, derivation_group_name)
      references merlin.external_source (key, derivation_group_name)
      on update cascade
      on delete cascade,
    constraint external_event_references_event_type_name
      foreign key (event_type_name)
      references merlin.external_event_type(name)
      on update cascade
      on delete restrict
);

comment on table merlin.external_event is e''
  'Externally imported events.';

comment on column merlin.external_event.key is e''
  'The key, or name, of the external_event.\n'
  'Part of the primary key, along with the source_key, derivation_group_name, and event_type_name.';
comment on column merlin.external_event.event_type_name is e''
  'The type of this external_event.';
comment on column merlin.external_event.source_key is e''
  'The key of the external_source that this external_event is included in.\n'
  'Used as a foreign key along with the derivation_group_name to directly identify said source.\n'
  'Part of the primary key along with the key, derivation_group_name, and event_type_name.';
comment on column merlin.external_event.derivation_group_name is e''
  'The derivation_group that the external_source bearing this external_event is a part of.';
comment on column merlin.external_event.start_time is e''
  'The start time (in _plan_ time, NOT planner time), of the range that this source describes.';
comment on column merlin.external_event.duration is e''
  'The span of time of this external event.';
comment on column merlin.external_event.metadata is e''
  'Any metadata or additional data associated with this version that a data originator may have wanted included.\n'
  'The metadata of an external event must follow the schema defined in the event''s event type (i.e., the columns ''metadata'' and ''metadata'').';

create trigger check_external_event_duration_is_nonnegative_trigger
before insert or update on merlin.external_event
for each row
when (new.duration < '0')
execute function util_functions.raise_duration_is_negative();

create function merlin.check_external_event_boundaries()
  returns trigger
  language plpgsql as $$
declare
  source_start timestamp with time zone;
  source_end timestamp with time zone;
  event_start timestamp with time zone;
  event_end timestamp with time zone;
begin
  select start_time, end_time into source_start, source_end
  from merlin.external_source
  where new.source_key = external_source.key
    and new.derivation_group_name = external_source.derivation_group_name;

  event_start := new.start_time;
  event_end := new.start_time + new.duration;
  if event_start < source_start or event_end > source_end then
    raise exception 'Event % out of bounds of source %.', new.key, new.source_key;
  end if;
  return new;
end;
$$;

-- Add a trigger verifying that metadata only contains defined metadata for the external event type
create or replace function merlin.validate_external_event_metadata()
  returns trigger
  language plpgsql as $$
declare
 event_metadata text[];
 event_invalid_metadata text[];
 event_type_required_metadata text[];
 event_type_valid_metadata text[];
begin
  event_metadata := (select array(select jsonb_object_keys(new.metadata)));
  select array(select jsonb_array_elements_text(required_metadata)) into event_type_required_metadata from merlin.external_event_type where new.event_type_name = external_event_type.name;
  select array(select jsonb_object_keys(metadata)) into event_type_valid_metadata from merlin.external_event_type where new.event_type_name = external_event_type.name;
  select array(select metadatum from unnest(event_metadata) as metadatum except select valid_metadata from unnest(event_type_valid_metadata) as valid_metadata) into event_invalid_metadata;
  if not (event_type_required_metadata <@ event_metadata) then
    raise exception 'External event does not contain all the required metadata for an event of type "%s"', new.event_type_name;
  end if;
  if array_length(event_invalid_metadata, 1) > 0 then
    raise exception 'External event contains metadata that are not defined within the event type "%s"', new.event_type_name;
  end if;
  return null;
end;
$$;

comment on function merlin.check_external_event_boundaries() is e''
  'Checks that an external_event added to the database has a start time and duration that fall in bounds of the associated external_source.';

create trigger check_external_event_boundaries
before insert on merlin.external_event
	for each row execute function merlin.check_external_event_boundaries();

comment on trigger check_external_event_boundaries on merlin.external_event is e''
  'Fires any time a new external event is added that checks that the span of the event fits in its referenced source.';
