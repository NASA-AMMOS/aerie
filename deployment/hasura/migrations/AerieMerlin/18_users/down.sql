--------------- TABLES ---------------
-- MERGE REQUEST
comment on column merge_request.requester is null;
comment on column merge_request.reviewer is null;

alter table public.merge_request
  add column requester_username text,
  add column reviewer_username text;

update public.merge_request mr
  set requester_username = u.username
  from metadata.users u where u.id = mr.requester;
update merge_request mr
  set reviewer_username = u.username
  from metadata.users u where u.id = mr.reviewer;

alter table public.merge_request
  alter column requester_username set not null,
  drop constraint merge_request_reviewer_exists,
  drop constraint merge_request_requester_exists,
  drop column requester,
  drop column reviewer;

comment on column merge_request.requester_username is e''
  'The username of the user who created this merge request.';
comment on column merge_request.reviewer_username is e''
  'The username of the user who reviews this merge request. Is empty until the request enters review.';

-- MERGE REQUEST COMMENT
comment on column merge_request_comment.commenter is null;

alter table public.merge_request_comment
  add column commenter_username text not null default '';

update public.merge_request_comment
  set commenter_username = u.username
  from metadata.users u
  where commenter = u.id;

alter table public.merge_request_comment
  drop constraint merge_request_commenter_exists,
  drop column commenter;

comment on column merge_request_comment.commenter_username is e''
  'The username of the user who left this comment.';

-- SIMULATION TEMPLATE
comment on column simulation_template.owner is null;

alter table public.simulation_template
  add column owner_name text not null default '';

update simulation_template
  set owner_name = u.username
  from metadata.users u
  where owner = u.id;

alter table public.simulation_template
  drop constraint simulation_template_owner_exists,
  drop column owner;
alter table public.simulation_template rename column owner_name to owner;

comment on column simulation_template.owner is e''
  'The user responsible for this simulation template';

-- SIMULATION_DATASET
comment on column simulation_dataset.requested_by is null;

alter table simulation_dataset
  add column requested_by_name text not null default '';

update simulation_dataset
  set requested_by_name = u.username
  from metadata.users u
  where requested_by = u.id;

alter table public.simulation_dataset
  drop constraint simulation_dataset_requested_by_exists,
  drop column requested_by;
alter table public.simulation_dataset rename column requested_by_name to requested_by;

comment on column simulation_dataset.requested_by is e''
  'The user who requested the simulation.';

-- PLAN COLLABORATORS
comment on column plan_collaborators.collaborator is null;

alter table public.plan_collaborators
  add column collaborator_name text,
  drop constraint plan_collaborators_pkey,
  add constraint plan_collaborators_pkey
    primary key (plan_id, collaborator);

update plan_collaborators
  set collaborator_name = u.username
  from metadata.users u
  where collaborator = u.id;

alter table public.plan_collaborators
  drop constraint plan_collaborator_collaborator_fkey,
  drop column collaborator;
alter table public.plan_collaborators rename column collaborator_name to collaborator;
delete from public.plan_collaborators
  where collaborator is null;
alter table public.plan_collaborators alter column collaborator set not null;

comment on column plan_collaborators.collaborator is e''
  'The username of the collaborator';

-- PLAN
comment on column plan.owner is null;
comment on column plan.updated_by is null;

alter table public.plan
  add column owner_name text not null default '',
  add column updated_by_name text not null default '';

update public.plan p
  set owner_name = u.username
  from metadata.users u where u.id = p.owner;
update public.plan p
  set updated_by_name = u.username
  from metadata.users u where u.id = p.updated_by;

alter table public.plan
  drop constraint plan_updated_by_exists,
  drop constraint plan_owner_exists,
  drop column updated_by,
  drop column owner;
alter table public.plan rename column owner_name to owner;
alter table public.plan rename column updated_by_name to updated_by;

comment on column plan.owner is e''
  'The user who owns the plan.';
comment on column plan.updated_by is e''
  'The user who last updated the plan.';

-- MISSION MODEL
comment on column mission_model.owner is null;

alter table public.mission_model
  add column owner_name text;

update mission_model
  set owner_name = u.username
  from metadata.users u
  where owner = u.id;

alter table public.mission_model
  drop constraint mission_model_owner_exists,
  drop column owner;
alter table public.mission_model rename column owner_name to owner;

comment on column mission_model.owner is e''
  'A human-meaningful identifier for the user responsible for this model.';

-- CONSTRAINTS
comment on column "constraint".owner is null;
comment on column "constraint".updated_by is null;

alter table public."constraint"
  add column owner_name text not null default '',
  add column updated_by_name text;

update public."constraint" c
  set owner_name = u.username
  from metadata.users u where u.id = c.owner;
update public."constraint" c
  set updated_by_name = u.username
  from metadata.users u where u.id = c.updated_by;

alter table public."constraint"
  drop constraint constraint_updated_by_exists,
  drop constraint constraint_owner_exists,
  drop column updated_by,
  drop column owner;
alter table public."constraint" rename column owner_name to owner;
alter table public."constraint" rename column updated_by_name to updated_by;

comment on column "constraint".owner is e''
  'The user responsible for this constraint.';
comment on column "constraint".updated_by is e''
  'The user who last modified this constraint.';

-- ACTIVITY PRESETS
comment on column activity_presets.owner is null;

alter table public.activity_presets
  add column owner_name text not null default '';

update public.activity_presets t
  set owner_name = u.username
  from metadata.users u
  where u.id = t.owner;

alter table public.activity_presets
  drop constraint activity_presets_owner_exists,
  drop column owner;
alter table public.activity_presets
  rename column owner_name to owner;

comment on column activity_presets.owner is e''
  'The owner of this activity preset';

-- TAGS
comment on column metadata.tags.owner is null;

alter table metadata.tags
  add column owner_name text;

update metadata.tags t
  set owner_name = u.username
  from metadata.users u
  where u.id = t.owner;
update metadata.tags
  set owner_name = ''
  where owner_name is null;

alter table metadata.tags
  drop constraint tags_owner_exists,
  drop column owner;
alter table metadata.tags
  rename owner_name to owner;
alter table metadata.tags
  alter column owner set not null;

comment on column metadata.tags.owner is e''
  'The user responsible for this tag. '
  '''Mission Model'' is used to represent tags originating from an uploaded mission model'
  '''Aerie Legacy'' is used to represent tags originating from a version of Aerie prior to this table''s creation.';

-- USERS AND ROLES VIEW
comment on view metadata.users_and_roles is null;
drop view metadata.users_and_roles;

-- USERS ALLOWED ROLES
comment on table metadata.users_allowed_roles is null;
drop table metadata.users_allowed_roles;

-- USERS
comment on column metadata.users.default_role is null;
comment on column metadata.users.username is null;
comment on column metadata.users.id is null;
comment on table metadata.users is null;
drop table metadata.users;

-- USER ROLES
comment on table metadata.user_roles is null;
drop table metadata.user_roles;

call migrations.mark_migration_rolled_back('18');
