-- Create a table to represent external event sources.
CREATE TABLE merlin.external_source (
    id integer NOT NULL,
    key text NOT NULL,
    file_id integer NOT NULL,
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

-- Add foreign key definition for file_id field, linking to uploaded_file table
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT "file_id -> uploaded_file" FOREIGN KEY (file_id) REFERENCES merlin.uploaded_file(id);


-- Add uniqueness constraint for key/derivation_group_id tuple (we exclude source_type_id as derivation_group inherently addresses that, being a subclass of source types)
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT logical_source_identifiers UNIQUE (key, derivation_group_id);

COMMENT ON CONSTRAINT logical_source_identifiers ON merlin.external_source IS 'The tuple (key, derivation_group_id) must be unique!';




-- Create table for external events
CREATE TABLE merlin.external_event (
    id integer NOT NULL,
    key text NOT NULL,
    event_type_id integer NOT NULL,
    source_id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    duration interval NOT NULL,
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


-- Add uniqueness constraint for key/source_id/event_type_id tuple
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT logical_event_identifiers UNIQUE (key, source_id, event_type_id);

COMMENT ON CONSTRAINT logical_event_identifiers ON merlin.external_event IS 'The tuple (key, event_type_id, and source_id) must be unique!';


-- Add foreign key linking the source_id to the id of an external_source entry
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT "source_id -> external_source.id" FOREIGN KEY (source_id) REFERENCES merlin.external_source(id);




-- Create table for external source types
CREATE TABLE merlin.external_source_type (
    id integer NOT NULL,
    name text NOT NULL
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

-- TODO: Come back to this when we want to tackle versioning
-- Add uniqueness constraint for name & version
--ALTER TABLE ONLY merlin.external_source_type
--    ADD CONSTRAINT logical_identifiers UNIQUE (name, version);
-- COMMENT ON CONSTRAINT logical_identifiers ON merlin.external_source_type IS 'The tuple (name, version) must be unique!';

-- Update merlin.external_source.source_type_id to link it to merlin.external_source_type.id
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT "source_type_id -> external_source_type" FOREIGN KEY (source_type_id) REFERENCES merlin.external_source_type(id);




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

-- Ensure that added external source's source type match this derivation group's source type; requires a trigger and a function!
CREATE OR REPLACE FUNCTION ensure_source_type_match()
RETURNS TRIGGER AS $$
BEGIN
  -- verify external_source.source_type_id = derivation_group.source_type_id
  PERFORM 1
  FROM merlin.derivation_group
  WHERE derivation_group.id = NEW.derivation_group_id AND derivation_group.source_type_id = NEW.source_type_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'The source type from the newly added source and the source type of the derivation group do not match.';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_source_type_match
BEFORE INSERT OR UPDATE ON merlin.external_source
FOR EACH ROW
EXECUTE FUNCTION ensure_source_type_match();




-- Create table for plan/external event links
CREATE TABLE merlin.plan_derivation_group (
    id integer NOT NULL,
    plan_id integer NOT NULL,
    derivation_group_id integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    owner text
);

COMMENT ON TABLE merlin.plan_derivation_group IS 'A table for linking externally imported event sources & plans.';

-- Ensure the id is serial
CREATE SEQUENCE merlin.plan_derivation_group_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.plan_derivation_group_id_seq OWNED BY merlin.plan_derivation_group.id;
ALTER TABLE ONLY merlin.plan_derivation_group ALTER COLUMN id SET DEFAULT nextval('merlin.plan_derivation_group_id_seq'::regclass);


-- Set primary key
ALTER TABLE ONLY merlin.plan_derivation_group
    ADD CONSTRAINT plan_derivation_group_pkey PRIMARY KEY (id);


-- Add uniqueness constraint for plan_id/external_source_id pair
ALTER TABLE ONLY merlin.plan_derivation_group
    ADD CONSTRAINT unique_plan_derivation_group UNIQUE (plan_id, derivation_group_id);

COMMENT ON CONSTRAINT unique_plan_derivation_group ON merlin.plan_derivation_group IS 'The tuple (plan_id, plan_derivation_group) must be unique!';

-- Add foreign keys
ALTER TABLE ONLY merlin.plan_derivation_group
    ADD CONSTRAINT fk_derivation_group_id FOREIGN KEY (derivation_group_id) REFERENCES merlin.derivation_group(id);

ALTER TABLE ONLY merlin.plan_derivation_group
    ADD CONSTRAINT fk_plan_id FOREIGN KEY (plan_id) REFERENCES merlin.plan(id);




-- Create table for external event types
CREATE TABLE merlin.external_event_type (
    id integer NOT NULL,
    name text NOT NULL
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

-- Set primary key
ALTER TABLE ONLY merlin.external_event_type
    ADD CONSTRAINT external_event_type_pkey PRIMARY KEY (id);

-- TODO: consider implementing versioning in a future batch of work

ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT "event_type_id -> external_event_type" FOREIGN KEY (event_type_id) REFERENCES merlin.external_event_type(id);




-- Create table mapping external sources to their contained event types
CREATE OR REPLACE VIEW merlin.external_source_event_type
 AS
 SELECT external_source.id AS external_source_id,
    array_agg(DISTINCT external_event.event_type_id) AS event_type_ids,
    array_agg(DISTINCT external_event_type.name) AS event_types
   FROM merlin.external_source
     JOIN merlin.external_event ON external_event.source_id = external_source.id
     JOIN merlin.external_event_type ON external_event_type.id = external_event.event_type_id
  GROUP BY external_source.id;

ALTER VIEW IF EXISTS merlin.external_source_event_type OWNER TO aerie;
COMMENT ON VIEW merlin.external_source_event_type
    IS 'Should be deprecated with the introduction of strict external source schemas, dictating allowable event types for given source types. But for now, this will do.';




-- Set primary key
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_source_event_type_pkey PRIMARY KEY (id);

-- Add key for external_event_type_id
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_event_type_id FOREIGN KEY (external_event_type_id) REFERENCES merlin.external_event_type(id);

-- Add key for external_source_id
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_source_id FOREIGN KEY (external_source_id) REFERENCES merlin.external_source(id);

call migrations.mark_migration_applied('9');
