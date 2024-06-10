-- Create table for plan/external event links
CREATE TABLE merlin.plan_external_source (
    id integer NOT NULL,
    plan_id integer NOT NULL,
    external_source_id integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    owner text
);

COMMENT ON TABLE merlin.plan_external_source IS 'A table for linking externally imported event sources & plans.';

-- Ensure the id is serial
CREATE SEQUENCE merlin.plan_external_source_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.plan_external_source_id_seq OWNED BY merlin.plan_external_source.id;
ALTER TABLE ONLY merlin.plan_external_source ALTER COLUMN id SET DEFAULT nextval('merlin.plan_external_source_id_seq'::regclass);


-- Set primary key
ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT plan_external_source_pkey PRIMARY KEY (id);


-- Add uniqueness constraint for plan_id/external_source_id pair
ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT unique_plan_external_source UNIQUE (plan_id, external_source_id);

COMMENT ON CONSTRAINT unique_plan_external_source ON merlin.plan_external_source IS 'The tuple (plan_id, external_source_id) must be unique!';

-- Add foreign keys
ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT fk_external_source_id FOREIGN KEY (external_source_id) REFERENCES merlin.external_source(id);

ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT fk_plan_id FOREIGN KEY (plan_id) REFERENCES merlin.plan(id);

call migrations.mark_migration_applied('7');
