create table "constraint" (
  id integer generated always as identity,

  name text not null,
  description text not null default '',
  definition text not null,

  plan_id integer null,
  model_id integer null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  owner text,
  updated_by text,

  constraint constraint_synthetic_key
    primary key (id),
  constraint constraint_owner_exists
    foreign key (owner)
    references metadata.users
    on update cascade
    on delete set null,
  constraint constraint_updated_by_exists
    foreign key (updated_by)
    references metadata.users
    on update cascade
    on delete set null,
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
comment on column "constraint".description is e''
  'A detailed description suitable for long-form documentation.';
comment on column "constraint".definition is e''
  'An executable expression in the Merlin constraint language.';
comment on column "constraint".plan_id is e''
  'The ID of the plan owning this constraint, if plan-scoped.';
comment on column "constraint".model_id is e''
  'The ID of the mission model owning this constraint, if model-scoped.';
comment on column "constraint".owner is e''
  'The user responsible for this constraint.';
comment on column "constraint".updated_by is e''
  'The user who last modified this constraint.';
comment on column "constraint".created_at is e''
  'The time at which this constraint was created.';
comment on column "constraint".updated_at is e''
  'The time at which this constraint was last modified.';


create or replace function constraint_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
before update on "constraint"
for each row
execute function constraint_set_updated_at();
