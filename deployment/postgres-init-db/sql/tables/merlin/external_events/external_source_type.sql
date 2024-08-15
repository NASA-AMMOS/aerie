-- Create table for external source types
CREATE TABLE merlin.external_source_type (
    id integer NOT NULL,
    name text NOT NULL UNIQUE,

    constraint external_source_type_pkey
      primary key (id)
);

COMMENT ON TABLE merlin.external_source_type IS 'A table for externally imported event source types.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_source_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_source_type_id_seq OWNED BY merlin.external_source_type.id;
ALTER TABLE ONLY merlin.external_source_type ALTER COLUMN id SET DEFAULT nextval('merlin.external_source_type_id_seq'::regclass);
