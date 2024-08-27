-- Create table for external events
create table merlin.external_event (
    key text not null,
    event_type_name text not null,
	  source_key text not null,
    derivation_group_name text not null,
    start_time timestamp with time zone not null,
    duration interval not null,
    properties jsonb,

    constraint external_event_pkey
      primary key (key, source_key, derivation_group_name, event_type_name),
    constraint external_event_references_source_key_derivation_group
      foreign key (source_key, derivation_group_name)
      references merlin.external_source (key, derivation_group_name),
    constraint external_event_references_event_type_name
      foreign key (event_type_name)
      references merlin.external_event_type(name)
);

comment on table merlin.external_event is e''
  'A table for externally imported events.';

-- Add a trigger verifying that events fit into their sources
create or replace function merlin.check_event_times()
 	returns trigger
 	language plpgsql as
$func$
declare
	source_start timestamp with time zone;
	source_end timestamp with time zone;
	event_start timestamp with time zone;
	event_end timestamp with time zone;
begin
  	select start_time into source_start from merlin.external_source where new.source_key = external_source.key and new.derivation_group_name = external_source.derivation_group_name;
  	select end_time into source_end from merlin.external_source where new.source_key = external_source.key AND new.derivation_group_name = external_source.derivation_group_name;
    event_start := new.start_time;
	event_end := new.start_time + new.duration;
	if event_start < source_start or event_end < source_start then
		raise exception 'Event %s out of bounds of source %s', new.key, new.source_key;
	end if;
	if event_start > source_end or event_end > source_end then
		raise exception 'Event %s out of bounds of source %s', new.key, new.source_key;
	end if;
	return null;
end;
$func$;

create trigger check_event_times
after insert on merlin.external_event
	for each row execute function merlin.check_event_times();
