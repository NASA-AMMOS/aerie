-- Create table for external source types
CREATE TABLE merlin.external_source_type (
    id integer NOT NULL,
    name text NOT NULL,
    description text NOT NULL,
    version integer NOT NULL,
    origin_info_schema jsonb
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

-- Set primary key
ALTER TABLE ONLY merlin.external_source_type
    ADD CONSTRAINT external_source_type_pkey PRIMARY KEY (id);

-- Add sequence for version... TODO, I think this has to happen outside of SQL..

-- Add uniqueness constraint for name & version
ALTER TABLE ONLY merlin.external_source_type
    ADD CONSTRAINT logical_identifiers UNIQUE (name, version);

COMMENT ON CONSTRAINT logical_identifiers ON merlin.external_source_type IS 'The tuple (name, version) must be unique!';
 
-- Create source_type_id field on external_source
ALTER TABLE merlin.external_source ADD source_type_id integer;
-- Drop old 'source_type' field
ALTER TABLE merlin.external_source DROP COLUMN source_type;
-- TODO: do we need to alter entries that already exist since this is a migration? i.e., if you have an old database and apply this migration, your EEs & ESs are going to be broken because source_type is being dropped and source_type_id is not being initialized...

-- Update merlin.external_source.source_type_id to link it to merlin.external_source_type.id
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT "source_type_id -> external_source_type" FOREIGN KEY (source_type_id) REFERENCES merlin.external_source_type(id);

call migrations.mark_migration_applied('6');