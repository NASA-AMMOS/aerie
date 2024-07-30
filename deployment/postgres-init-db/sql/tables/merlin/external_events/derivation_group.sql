-- Create a table to represent derivation groups for external sources
CREATE TABLE merlin.derivation_group (
    id integer NOT NULL,
    name text NOT NULL,
    source_type_id integer NOT NULL
);

COMMENT ON TABLE merlin.derivation_group IS 'A table to represent the names of groups of sources to run derivation operations over.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.derivation_group_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.derivation_group_id_seq OWNED BY merlin.derivation_group.id;
ALTER TABLE ONLY merlin.derivation_group ALTER COLUMN id SET DEFAULT nextval('merlin.derivation_group_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.derivation_group
    ADD CONSTRAINT derivation_group_pkey PRIMARY KEY (id);

-- Add foreign key definition for derivation_group_id field, linking to derivation_group table
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT "derivation_group_id -> derivation_group" FOREIGN KEY (derivation_group_id) REFERENCES merlin.derivation_group(id);

-- Add foreign key definition for source_type_id field, linking to external_source_type table
ALTER TABLE ONLY merlin.derivation_group
    ADD CONSTRAINT "source_type_id -> external_source_type" FOREIGN KEY (source_type_id) REFERENCES merlin.external_source_type(id);
