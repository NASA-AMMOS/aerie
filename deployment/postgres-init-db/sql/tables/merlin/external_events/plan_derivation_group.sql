create table merlin.plan_derivation_group (
    plan_id integer not null,
    derivation_group_name text not null,
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

comment on column merlin.plan_derivation_group.plan_id is e''
  'The id of the plan that the derivation_group (referenced by derivation_group_name) in this link is being associated with.';
comment on column merlin.plan_derivation_group.derivation_group_name is e''
  'The name of the derivation group that is being associated with the plan (referenced by plan_id) in this link.';
comment on column merlin.plan_derivation_group.created_at is e''
  'The time (in _planner_ time, NOT _plan_ time) that this link was created at.';
