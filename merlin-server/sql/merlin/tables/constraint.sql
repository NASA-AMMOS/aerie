create table "constraint" (
  id integer generated always as identity,

  name text not null,
  summary text not null,
  description text not null,
  definition text not null,

  plan_id integer null,
  model_id integer null,

  constraint constraint_synthetic_key
    primary key (id),
  constraint constraint_scoped_to_plan
    foreign key (plan_id)
    references plan
    on update cascade
    on delete cascade,
  constraint constraint_scoped_to_model
    foreign key (model_id)
    references mission_model
    on update cascade
    on delete cascade,
  constraint constraint_has_one_scope
    check (
      -- Model-scoped
      (plan_id is null     and model_id is not null) or
      -- Plan-scoped
      (plan_id is not null and model_id is null)
    )
);

comment on table "constraint" is e''
  'A constraint associated with an individual plan.';

comment on column "constraint".id is e''
  'The synthetic identifier for this constraint.';
comment on column "constraint".name is e''
  'A human-meaningful name.';
comment on column "constraint".summary is e''
  'A short summary suitable for use in a tooltip or compact list.';
comment on column "constraint".description is e''
  'A detailed description suitable for long-form documentation.';
comment on column "constraint".definition is e''
  'An executable expression in the Merlin constraint language.';
comment on column "constraint".plan_id is e''
  'The ID of the plan owning this constraint, if plan-scoped.';
comment on column "constraint".model_id is e''
  'The ID of the mission model owning this constraint, if model-scoped.';
