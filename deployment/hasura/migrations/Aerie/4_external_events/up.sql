-- Create table for external events
CREATE TABLE merlin.external_event (
    id integer NOT NULL,
    key text NOT NULL,
    event_type text NOT NULL,
    source_id integer NOT NULL,
    "time" timestamp with time zone NOT NULL,
    duration time without time zone NOT NULL,
    properties jsonb
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


-- Set primary key
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT external_event_pkey PRIMARY KEY (id);


-- Add uniqueness constraint for key/source_id/event_type tuple
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT logical_identifiers UNIQUE (key, source_id, event_type);

COMMENT ON CONSTRAINT logical_identifiers ON merlin.external_event IS 'The tuple (key, event_type, and source_id) must be unique!';


-- Add foreign key linking the source_id to the id of an external_source entry
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT "source_id -> external_source.id" FOREIGN KEY (source_id) REFERENCES merlin.external_source(id);

call migrations.mark_migration_applied('4');
