-- Create table for external event types
CREATE TABLE merlin.external_event_type (
    id integer NOT NULL,
    name text NOT NULL UNIQUE,

    constraint external_event_type_pkey
      primary key (id)

);

COMMENT ON TABLE merlin.external_event_type IS 'A table for externally imported event types.';

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
