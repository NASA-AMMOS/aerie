-- Create table for external event types
CREATE TABLE merlin.external_event_type (
    id integer NOT NULL,
    name text NOT NULL
);

COMMENT ON TABLE merlin.external_event_type IS 'A table for externall imported event types.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_event_type_id_seq
    AS integer
    START WITH 1
    INCREMENT by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_event_type_id_seq OWNED BY merlin.external_event_type.id;
ALTER TABLE ONLY merlin.external_event_type ALTER COLUMN id SET DEFAULT nextval('merlin.external_event_type_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.external_event_type
    ADD CONSTRAINT external_event_type_pkey PRIMARY KEY (id);

-- TODO: consider implementing versioning in a future batch of work

ALTER TABLE ONLY merlin.external_event_type
    ADD CONSTRAINT "event_type_id -> external_event_type" FOREIGN KEY (event_type_id) REFERENCES merlin.external_event_type(id);