-- Activity Type
alter table activity_type
  add column subsystem text null;
comment on column activity_type.subsystem is e''
  'The subsystem this activity type belongs to.';

-- Constraints
update "constraint"
  set description = description || '\n' || summary
  where not summary = '';

comment on column "constraint".summary is null;

alter table "constraint"
  drop column summary,
  add column tags text[] default '{}',
  add column owner text not null default '',
  add column updated_by text not null default '',
  add column created_at timestamptz not null default now(),
  add column updated_at timestamptz not null default now();

comment on column "constraint".tags is e''
  'The tags associated with this constraint.';
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

-- Mission Model
alter table mission_model
  add column description text not null default '',
  add column created_at timestamptz not null default now();

comment on column mission_model.description is e''
  'A human-meaningful description of the mission model.';
comment on column mission_model.created_at is e''
  'The time this mission model was uploaded into Aerie.';

-- Plan
alter table plan
  add column created_at timestamptz not null default now(),
  add column updated_at timestamptz not null default now(),
  add column updated_by text not null default '',
  add column owner text not null default '';

comment on column plan.created_at is e''
  'The time at which this plan was created.';
comment on column plan.updated_at is e''
  'The time at which this plan was last updated.';
comment on column plan.updated_by is e''
  'The user who last updated the plan.';
comment on column plan.owner is e''
  'The user who owns the plan.';

create function plan_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
  before update or insert on plan
  for each row
execute function plan_set_updated_at();

-- Plan Collaborators
create table plan_collaborators(
  plan_id int not null,
  collaborator text not null,

  constraint plan_collaborators_pkey
    primary key (plan_id, collaborator),
  constraint plan_collaborators_plan_id_fkey
    foreign key (plan_id) references plan
    on update cascade
    on delete cascade
);

comment on table plan_collaborators is e''
  'A collection of users who collaborate on the plan alongside the plan''s owner.';
comment on column plan_collaborators.plan_id is e''
  'The plan the user is a collaborator on.';
comment on column plan_collaborators.collaborator is e''
  'The username of the collaborator';


-- Simulation Dataset
alter table simulation_dataset
  add column requested_by text not null default '',
  add column requested_at timestamptz not null default now();

comment on column simulation_dataset.requested_by is e''
  'The user who requested the simulation.';
comment on column simulation_dataset.requested_at is e''
  'When this simulation dataset was created.';

call migrations.mark_migration_applied('12');
