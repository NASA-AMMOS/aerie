-- Create a table to represent external event sources.
CREATE TABLE merlin.external_source (
    id integer NOT NULL,
    key text NOT NULL,
    source_type_id integer NOT NULL,
    derivation_group_id integer NOT NULL,
    valid_at timestamp with time zone NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    metadata jsonb
);

COMMENT ON TABLE merlin.external_source IS 'A table for externally imported event sources.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_source_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_source_id_seq OWNED BY merlin.external_source.id;
ALTER TABLE ONLY merlin.external_source ALTER COLUMN id SET DEFAULT nextval('merlin.external_source_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT external_source_pkey PRIMARY KEY (id);

-- Add uniqueness constraint for key/derivation_group_id tuple (we exclude source_type_id as derivation_group inherently addresses that, being a subclass of source types)
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT logical_source_identifiers UNIQUE (key, derivation_group_id);

COMMENT ON CONSTRAINT logical_source_identifiers ON merlin.external_source IS 'The tuple (key, derivation_group_id) must be unique!';
