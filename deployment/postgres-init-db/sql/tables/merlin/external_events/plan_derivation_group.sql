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
