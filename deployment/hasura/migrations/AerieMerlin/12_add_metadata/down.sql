-- Simulation Dataset
comment on column simulation_dataset.requested_by is null;
comment on column simulation_dataset.requested_at is null;
alter table simulation_dataset
  drop column requested_by,
  drop column requested_at;

-- Plan Collaborators
comment on column plan_collaborators.collaborator is null;
comment on column plan_collaborators.plan_id is null;
comment on table plan_collaborators is null;

drop table plan_collaborators;

-- Plan
drop trigger set_timestamp on plan;
drop function plan_set_updated_at();

comment on column plan.updated_by is null;
comment on column plan.updated_at is null;
comment on column plan.created_at is null;
comment on column plan.owner is null;

alter table plan
  drop column updated_by,
  drop column updated_at,
  drop column created_at,
  drop column owner;

-- Mission Model
comment on column mission_model.created_at is null;
comment on column mission_model.description is null;

alter table mission_model
  drop column created_at,
  drop column description;

-- Constraints
drop trigger set_timestamp on "constraint";
drop function constraint_set_updated_at();

comment on column "constraint".updated_at is null;
comment on column "constraint".created_at is null;
comment on column "constraint".updated_by is null;
comment on column "constraint".owner is null;
comment on column "constraint".tags is null;

alter table "constraint"
  drop column updated_at,
  drop column created_at,
  drop column updated_by,
  drop column owner,
  drop column tags,
  add column summary text not null;

comment on column "constraint".summary is e''
  'A short summary suitable for use in a tooltip or compact list.';

-- Activity Type
comment on column activity_type.subsystem is null;
alter table activity_type
  drop column subsystem;

call migrations.mark_migration_rolled_back('12');
