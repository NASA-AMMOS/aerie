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
CREATE OR REPLACE FUNCTION merlin.check_event_times()
 	RETURNS TRIGGER
 	LANGUAGE plpgsql AS
$func$
DECLARE
	source_start timestamp with time zone;
	source_end timestamp with time zone;
	event_start timestamp with time zone;
	event_end timestamp with time zone;
BEGIN
  	SELECT start_time INTO source_start FROM merlin.external_source WHERE NEW.source_key = external_source.key AND NEW.derivation_group_name = external_source.derivation_group_name;
  	SELECT end_time INTO source_end FROM merlin.external_source WHERE NEW.source_key = external_source.key AND NEW.derivation_group_name = external_source.derivation_group_name;
    event_start := NEW.start_time;
	event_end := NEW.start_time + NEW.duration;
	IF event_start < source_start OR event_end < source_start THEN
		RAISE EXCEPTION 'Event %s out of bounds of source %s', NEW.key, NEW.source_key;
	END IF;
	IF event_start > source_end OR event_end > source_end THEN
		RAISE EXCEPTION 'Event %s out of bounds of source %s', NEW.key, NEW.source_key;
	END IF;
	RETURN NULL;
END;
$func$;

ALTER FUNCTION merlin.check_event_times()
    OWNER TO aerie;
GRANT EXECUTE ON FUNCTION merlin.check_event_times() TO aerie;
CREATE OR REPLACE TRIGGER check_event_times
AFTER INSERT ON merlin.external_event
	FOR EACH ROW EXECUTE FUNCTION merlin.check_event_times();
