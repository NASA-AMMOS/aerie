-- Create a table to represent external event sources.
CREATE TABLE merlin.external_source (
    id integer NOT NULL,
    key text NOT NULL,
    source_type_name text NOT NULL,
    derivation_group_name text NOT NULL,
    valid_at timestamp with time zone NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    metadata jsonb,

    constraint external_source_pkey
      primary key (id),
    constraint external_source_references_external_source_type_name
      foreign key (source_type_name)
      references merlin.external_source_type(name),
    constraint external_source_type_matches_derivation_group
      foreign key (derivation_group_name, source_type_name)
      references merlin.derivation_group (name, source_type_name)
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

-- Add uniqueness constraint for key/derivation_group_name tuple (we exclude source_type_name as derivation_group inherently addresses that, being a subclass of source types)
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT logical_source_identifiers UNIQUE (key, derivation_group_name);

COMMENT ON CONSTRAINT logical_source_identifiers ON merlin.external_source IS 'The tuple (key, derivation_group_name) must be unique!';
