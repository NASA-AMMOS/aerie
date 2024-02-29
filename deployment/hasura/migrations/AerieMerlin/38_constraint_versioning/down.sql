/**** ADD CONSTRAINTS ****/
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

create function constraint_check_constraint_run()
  returns trigger
  security definer
  language plpgsql as $$
begin
  update constraint_run
  set definition_outdated = true
  where constraint_id = new.id
    and constraint_definition != new.definition
    and definition_outdated = false;
  update constraint_run
  set definition_outdated = false
  where constraint_id = new.id
    and constraint_definition = new.definition
    and definition_outdated = true;
  return new;
end$$;
create trigger constraint_check_constraint_run_trigger
  after update on "constraint"
  for each row
  when (new.definition != old.definition)
execute function constraint_check_constraint_run();

-- Timestamp Trigger will be added after data migration for data consistency

/****** TAGS *******/
drop table metadata.constraint_definition_tags;

alter table metadata.constraint_tags drop constraint constraint_tags_constraint_id_fkey;
alter table metadata.constraint_tags add foreign key (constraint_id)
  references "constraint"
    on update cascade
    on delete cascade;

/****** CONSTRAINT RUN ******/
-- Clear cache
truncate table constraint_run;

alter table constraint_run
drop constraint constraint_run_key,
drop constraint constraint_run_to_constraint_definition,
drop column constraint_revision,
add column definition_outdated boolean default false not null,
add column constraint_definition text not null,
add constraint constraint_run_to_constraint
  foreign key (constraint_id)
      references "constraint"
      on delete cascade,
add constraint constraint_run_key
  primary key (constraint_id, constraint_definition, simulation_dataset_id);

comment on column constraint_run.constraint_definition is e''
  'The definition of the constraint when it was checked, used to determine staleness.';
comment on column constraint_run.definition_outdated is e''
  'Tracks if the constraint definition is outdated because the constraint has been changed.';

/******* DATA MIGRATION *******/
-- Add  model constraints
-- Because multiple models may be using the same constraint/constraint definition, we have to regenerate the constraint's id
with specified_definition(constraint_id, model_id, definition, definition_creation) as (
  select cd.constraint_id, cd.revision, ms.model_id, cd.definition, cd.created_at
    from constraint_model_specification ms
      left join constraint_definition cd using (constraint_id)
    where (ms.constraint_revision is not null and ms.constraint_revision = cd.revision)
       or (ms.constraint_revision is null and cd.revision = (select def.revision
                                                             from constraint_definition def
                                                             where def.constraint_id = ms.constraint_id
                                                             order by def.revision desc limit 1)))
insert into "constraint"(name, description, definition, model_id, created_at, updated_at, owner, updated_by)
select cm.name, cm.description, sd.definition, sd.model_id, cm.created_at, greatest(cm.updated_at::timestamptz, sd.definition_creation::timestamptz), cm.owner, cm.updated_by
  from specified_definition sd
  left join constraint_metadata cm on cm.id = sd.constraint_id;

-- Add plan constraints
-- Because multiple plans may be using the same constraint/constraint definition, we have to regenerate the constraint's id
with
  specified_plan_definition(constraint_id, revision, plan_id, definition, definition_creation) as (
    select cd.constraint_id, cd.revision, s.plan_id, cd.definition, cd.created_at
    from constraint_specification s
      left join constraint_definition cd using (constraint_id)
    where (s.constraint_revision is not null and s.constraint_revision = cd.revision)
       or (s.constraint_revision is null and cd.revision = (select def.revision
                                                            from constraint_definition def
                                                            where def.constraint_id = s.constraint_id
                                                            order by def.revision desc limit 1))),
  specified_model_definition(constraint_id, revision, plan_id, definition, definition_creation) as (
    select cd.constraint_id, cd.revision, p.id, cd.definition, cd.created_at
    from constraint_model_specification cms
      left join constraint_definition cd using (constraint_id)
      left join plan p using (model_id)
    where (cms.constraint_revision is not null and cms.constraint_revision = cd.revision)
       or (cms.constraint_revision is null and cd.revision = (select def.revision
                                                              from constraint_definition def
                                                              where def.constraint_id = cms.constraint_id
                                                              order by def.revision desc limit 1)))
insert into "constraint"(name, description, definition, plan_id, created_at, updated_at, owner, updated_by)
  select cm.name, cm.description, pd.definition, pd.plan_id, cm.created_at,
         greatest(cm.updated_at::timestamptz, pd.definition_creation::timestamptz), cm.owner, cm.updated_by
  from specified_plan_definition pd
    left join constraint_metadata cm on cm.id = pd.constraint_id
-- Exclude constraints that match the model spec
except
  select cm.name, cm.description, md.definition, md.plan_id, cm.created_at,
         greatest(cm.updated_at::timestamptz, md.definition_creation::timestamptz), cm.owner, cm.updated_by
  from specified_model_definition md
    left join constraint_metadata cm on cm.id = md.constraint_id;

-- Add timestamp trigger for future entries
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

/****** NEW TABLES ******/
/*-- PLAN TRIGGER --*/
drop trigger populate_constraint_spec_new_plan_trigger on plan;
drop function populate_constraint_spec_new_plan();

/*-- CONSTRAINT MODEL SPECIFICATION --*/
drop table constraint_model_specification;

/*-- CONSTRAINT SPECIFICATION --*/
drop table constraint_specification;

/*-- CONSTRAINT DEFINITION --*/
drop trigger constraint_definition_set_revision on constraint_definition;
drop function constraint_definition_set_revision();
drop table constraint_definition;

/*-- CONSTRAINT METADATA --*/
drop trigger set_timestamp on constraint_metadata;
drop function constraint_metadata_set_updated_at();

drop index name_unique_if_published;
drop table constraint_metadata;

call migrations.mark_migration_rolled_back('38');
