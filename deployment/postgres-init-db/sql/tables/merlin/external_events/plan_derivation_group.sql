-- Create table for plan/external event links
create table merlin.plan_derivation_group (
    plan_id integer not null,
    derivation_group_name text not null,
    enabled boolean not null default true,
    created_at timestamp with time zone default now() not null,

    constraint plan_derivation_group_pkey
      primary key (plan_id, derivation_group_name),
    constraint plan_derivation_group_references_plan_id
      foreign key (plan_id)
      references merlin.plan(id),
    constraint plan_derivation_group_references_derivation_group_name
      foreign key (derivation_group_name)
      references merlin.derivation_group(name)
);

comment on table merlin.plan_derivation_group is e''
  'A table for linking externally imported event sources & plans.';
