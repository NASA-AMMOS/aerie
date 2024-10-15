create table merlin.plan_derivation_group (
    plan_id integer not null,
    derivation_group_name text not null,
    last_acknowledged_at timestamp with time zone default now() not null,

    constraint plan_derivation_group_pkey
      primary key (plan_id, derivation_group_name),
    constraint pdg_plan_exists
      foreign key (plan_id)
      references merlin.plan(id)
      on delete cascade,
    constraint pdg_derivation_group_exists
      foreign key (derivation_group_name)
      references merlin.derivation_group(name)
      on update cascade
      on delete restrict
);

comment on table merlin.plan_derivation_group is e''
  'Links externally imported event sources & plans.\n'
  'Additionally, tracks the last time a plan owner/contributor(s) have acknowledged additions to the derivation group.\n';

comment on column merlin.plan_derivation_group.plan_id is e''
  'The plan with which the derivation group is associated.';
comment on column merlin.plan_derivation_group.derivation_group_name is e''
  'The derivation group being associated with the plan.';
comment on column merlin.plan_derivation_group.last_acknowledged_at is e''
  'The time at which changes to the derivation group were last acknowledged.';
