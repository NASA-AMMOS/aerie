-- Create table for external events
CREATE TABLE merlin.external_event (
    id integer NOT NULL,
    key text NOT NULL,
    event_type_name text NOT NULL,
    source_id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    duration interval NOT NULL,
    properties jsonb,

    constraint external_event_pkey
      primary key (id),
    constraint external_event_references_source_id
      foreign key (source_id)
      references merlin.external_source(id),
    constraint external_event_references_event_type_name
      foreign key (event_type_name)
      references merlin.external_event_type(name)
);

COMMENT ON TABLE merlin.external_event IS 'A table for externally imported events.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_event_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_event_id_seq OWNED BY merlin.external_event.id;
ALTER TABLE ONLY merlin.external_event ALTER COLUMN id SET DEFAULT nextval('merlin.external_event_id_seq'::regclass);

-- Add uniqueness constraint for key/source_id/event_type_name tuple
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT logical_event_identifiers UNIQUE (key, source_id, event_type_name);

COMMENT ON CONSTRAINT logical_event_identifiers ON merlin.external_event IS 'The tuple (key, event_type_name, and source_id) must be unique!';
