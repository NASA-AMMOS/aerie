-- Create table for plan/external event links
CREATE TABLE merlin.plan_derivation_group (
    id integer NOT NULL,
    plan_id integer NOT NULL,
    derivation_group_name text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,

    constraint plan_derivation_group_pkey
      primary key (id),
    constraint plan_derivation_group_references_plan_id
      foreign key (plan_id)
      references merlin.plan(id),
    constraint plan_derivation_group_references_derivation_group_name
      foreign key (derivation_group_name)
      references merlin.derivation_group(name),
    constraint unique_plan_derivation_group
      unique (plan_id, derivation_group_name)
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

COMMENT ON CONSTRAINT unique_plan_derivation_group ON merlin.plan_derivation_group IS 'The tuple (plan_id, plan_derivation_group) must be unique!';
