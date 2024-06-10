-- Create a table to represent external event sources.
CREATE TABLE merlin.external_source (
    id integer NOT NULL,
    key text NOT NULL,
    file_id integer NOT NULL,
    source_type text NOT NULL,
    valid_at timestamp with time zone NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
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

-- Add foreign key definition for file_id field, linking to uploaded_file table
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT "file_id -> uploaded_file" FOREIGN KEY (file_id) REFERENCES merlin.uploaded_file(id);

call migrations.mark_migration_applied('5');
