--------------- TABLES ---------------
-- MERGE REQUEST
comment on column merge_request.requester_username is null;
comment on column merge_request.reviewer_username is null;

alter table public.merge_request
  drop constraint merge_request_reviewer_exists,
  drop constraint merge_request_requester_exists;
update public.merge_request
  set requester_username = ''
  where requester_username is null;
alter table public.merge_request
  alter column requester_username set not null;

comment on column merge_request.requester_username is e''
  'The username of the user who created this merge request.';
comment on column merge_request.reviewer_username is e''
  'The username of the user who reviews this merge request. Is empty until the request enters review.';

-- MERGE REQUEST COMMENT
comment on column merge_request_comment.commenter_username is null;

alter table public.merge_request_comment
  drop constraint merge_request_commenter_exists,
  alter column commenter_username set default '';
update public.merge_request_comment
  set commenter_username = default
  where commenter_username is null;
alter table public.merge_request_comment
  alter column commenter_username set not null;

comment on column merge_request_comment.commenter_username is e''
  'The username of the user who left this comment.';

-- SIMULATION TEMPLATE
alter table public.simulation_template
  drop constraint simulation_template_owner_exists,
  alter column owner set default '';

update simulation_template
  set owner = default
  where owner is null;

alter table public.simulation_template
  alter column owner set not null;

-- SIMULATION DATASET
alter table simulation_dataset
  drop constraint simulation_dataset_requested_by_exists,
  alter column requested_by set default '';

update simulation_dataset
  set requested_by = default
  where requested_by is null;

alter table simulation_dataset
  alter column requested_by set not null;

-- PLAN COLLABORATORS
alter table public.plan_collaborators
  drop constraint plan_collaborator_collaborator_fkey;

-- PLAN
alter table public.plan
  drop constraint plan_updated_by_exists,
  drop constraint plan_owner_exists,
  alter column updated_by set default '',
  alter column owner set default '';

update public.plan
  set owner = default
  where owner is null;
update public.plan
  set updated_by = default
  where updated_by is null;

alter table public.plan
  alter column updated_by set not null,
  alter column owner set not null;

-- MISSION MODEL
alter table public.mission_model
  drop constraint mission_model_owner_exists;

-- CONSTRAINTS
alter table public."constraint"
  drop constraint constraint_updated_by_exists,
  drop constraint constraint_owner_exists,
  alter column updated_by set default '',
  alter column owner set default '';
update public."constraint"
  set owner = default
  where owner is null;
update public."constraint"
  set updated_by = default
  where updated_by is null;
alter table public."constraint"
  alter column updated_by drop not null,
  alter column owner set not null;

-- ACTIVITY PRESETS
alter table public.activity_presets
  drop constraint activity_presets_owner_exists,
  alter column owner set default '';
update public.activity_presets
  set owner = default
  where owner is null;
alter table public.activity_presets
  alter column owner set not null;

-- TAGS
alter table metadata.tags
  drop constraint tags_owner_exists,
  alter column owner set default '';
update metadata.tags
  set owner = default
  where owner is null;
alter table metadata.tags
  alter column owner set not null;

-- USERS AND ROLES VIEW
comment on view metadata.users_and_roles is null;
drop view metadata.users_and_roles;

-- USERS ALLOWED ROLES
comment on table metadata.users_allowed_roles is null;
drop table metadata.users_allowed_roles;

-- USERS
comment on column metadata.users.default_role is null;
comment on column metadata.users.username is null;
comment on table metadata.users is null;
drop table metadata.users;

-- USER ROLES
comment on table metadata.user_roles is null;
drop table metadata.user_roles;

call migrations.mark_migration_rolled_back('19');
