-- Create table mapping external sources to their contained event types
CREATE TABLE merlin.external_source_event_types (
    id integer NOT NULL,
    external_source_id integer NOT NULL,
    external_event_type_id integer NOT NULL
);

COMMENT ON TABLE merlin.external_source_event_types IS 'A table detailing the event types that a given external_source has.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_source_event_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE merlin.external_source_event_types_id_seq OWNED BY merlin.external_source_event_types.id;
ALTER TABLE ONLY merlin.external_source_event_types ALTER COLUMN id SET DEFAULT nextval('merlin.external_source_event_types_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_source_event_type_pkey PRIMARY KEY (id);

-- Add key for external_event_type_id
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_event_type_id FOREIGN KEY (external_event_type_id) REFERENCES merlin.external_event_type(id);

-- Add key for external_source_id
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_source_id FOREIGN KEY (external_source_id) REFERENCES merlin.external_source(id);

call migrations.mark_migration_applied('10');
