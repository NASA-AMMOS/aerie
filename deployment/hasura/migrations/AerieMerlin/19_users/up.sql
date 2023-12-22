-- USER ROLES
-- This table is an enum-compatible table (https://hasura.io/docs/latest/schema/postgres/enums/#pg-create-enum-table)
create table metadata.user_roles(
  role text primary key,
  description text null
);
insert into metadata.user_roles(role) values ('admin'), ('user'), ('viewer');

comment on table metadata.user_roles is e''
  'A list of all the allowed Hasura roles, with an optional description per role';

-- USERS
create table metadata.users(
  username text not null primary key,
  default_role text not null references metadata.user_roles
    on update cascade
    on delete restrict
);

comment on table metadata.users is e''
'All users recognized by this deployment.';
comment on column metadata.users.username is e''
'The user''s username. A unique identifier for this user.';
comment on column metadata.users.default_role is e''
'The user''s default role for making Hasura requests.';

-- USERS ALLOWED ROLES
create table metadata.users_allowed_roles(
  username text references metadata.users
    on update cascade
    on delete cascade,
  allowed_role text not null references metadata.user_roles
    on update cascade
    on delete cascade,

  primary key (username, allowed_role),

  constraint system_roles_have_no_allowed_roles
    check (username != 'Mission Model' and username != 'Aerie Legacy' )
);

comment on table metadata.users_allowed_roles is e''
'An association between a user and all of the roles they are allowed to use for Hasura requests';

-- USERS AND ROLES VIEW
create view metadata.users_and_roles as
(
  select
    u.username as username,
    -- Roles
    u.default_role as hasura_default_role,
    array_agg(r.allowed_role) filter (where r.allowed_role is not null) as hasura_allowed_roles
  from metadata.users u
  left join metadata.users_allowed_roles r using (username)
  group by u.username
);

comment on view metadata.users_and_roles is e''
'View a user''s information with their role information';

-- TAGS
alter table metadata.tags
  alter column owner drop not null,
  alter column owner drop default;
update metadata.tags t
  set owner = null
  where owner = '';

insert into metadata.users(username, default_role)
  select owner, 'user' from metadata.tags
  where owner is not null
  on conflict (username) do nothing;

alter table metadata.tags
  add constraint tags_owner_exists
    foreign key (owner) references metadata.users
    on update cascade
    on delete set null;

-- ACTIVITY PRESETS
alter table public.activity_presets
  alter column owner drop not null,
  alter column owner drop default;
update public.activity_presets
  set owner = null
  where owner = '';

insert into metadata.users(username, default_role)
  select owner, 'user' from public.activity_presets
  where owner is not null
  on conflict (username) do nothing;

alter table public.activity_presets
  add constraint activity_presets_owner_exists
    foreign key (owner) references metadata.users
    on update cascade
    on delete set null;

-- CONSTRAINTS
alter table public."constraint"
  alter column owner drop not null,
  alter column owner drop default,
  alter column updated_by drop not null,
  alter column updated_by drop default;
update public."constraint"
  set owner = null
  where owner = '';
update public."constraint"
  set updated_by = null
  where updated_by = '';

insert into metadata.users(username, default_role)
    select owner, 'user' from public."constraint"
    where owner is not null
  union distinct
    select updated_by, 'user' from public."constraint"
    where updated_by is not null
  on conflict (username) do nothing;

alter table public."constraint"
  add constraint constraint_owner_exists
    foreign key (owner)
    references metadata.users
    on update cascade
    on delete set null,
  add constraint constraint_updated_by_exists
    foreign key (updated_by)
    references metadata.users
    on update cascade
    on delete set null;

-- MISSION MODEL
update public.mission_model
  set owner = null
  where owner = '';

insert into metadata.users(username, default_role)
    select owner, 'user' from public.mission_model
    where owner is not null
  on conflict (username) do nothing;

alter table public.mission_model
  add constraint mission_model_owner_exists
    foreign key (owner) references metadata.users
    on update cascade
    on delete set null;

-- PLAN
alter table public.plan
  alter column owner drop not null,
  alter column owner drop default,
  alter column updated_by drop not null,
  alter column updated_by drop default;

update public.plan
  set owner = null
  where owner = '';
update public.plan
  set updated_by = null
  where updated_by = '';

insert into metadata.users(username, default_role)
    select owner, 'user' from public.plan
    where owner is not null
  union distinct
    select updated_by, 'user' from public.plan
    where updated_by is not null
  on conflict (username) do nothing;

alter table public.plan
  add constraint plan_owner_exists
    foreign key (owner)
    references metadata.users
    on update cascade
    on delete set null,
  add constraint plan_updated_by_exists
    foreign key (updated_by)
    references metadata.users
    on update cascade
    on delete set null;

-- PLAN COLLABORATORS
delete from public.plan_collaborators
  where collaborator = '';

insert into metadata.users(username, default_role)
    select collaborator, 'user' from public.plan_collaborators
  on conflict (username) do nothing;

alter table public.plan_collaborators
  add constraint plan_collaborator_collaborator_fkey
    foreign key (collaborator) references metadata.users
    on update cascade
    on delete cascade;

-- SIMULATION DATASET
alter table simulation_dataset
  alter column requested_by drop not null,
  alter column requested_by drop default;

update simulation_dataset
  set requested_by = null
  where requested_by = '';

insert into metadata.users(username, default_role)
    select requested_by, 'user' from public.simulation_dataset
    where requested_by is not null
  on conflict (username) do nothing;

alter table simulation_dataset
  add constraint simulation_dataset_requested_by_exists
    foreign key (requested_by) references metadata.users
    on update cascade
    on delete set null;

-- SIMULATION TEMPLATE
alter table public.simulation_template
  alter column owner drop not null,
  alter column owner drop default;

update simulation_template
  set owner = null
  where owner = '';

insert into metadata.users(username, default_role)
    select owner, 'user' from public.simulation_template
    where owner is not null
  on conflict (username) do nothing;

alter table public.simulation_template
  add constraint simulation_template_owner_exists
    foreign key (owner)
    references metadata.users
    on update cascade
    on delete set null;

-- MERGE REQUEST COMMENT
comment on column merge_request_comment.commenter_username is null;

alter table public.merge_request_comment
  alter column commenter_username drop not null,
  alter column commenter_username drop default;

update public.merge_request_comment
  set commenter_username = null
  where commenter_username = '';

insert into metadata.users(username, default_role)
    select commenter_username, 'user' from public.merge_request_comment
    where not commenter_username is not null
  on conflict (username) do nothing;

alter table public.merge_request_comment
  add constraint merge_request_commenter_exists
    foreign key (commenter_username)
    references metadata.users
    on update cascade
    on delete set null;

comment on column merge_request_comment.commenter_username is e''
  'The user who left this comment.';

-- MERGE REQUEST
comment on column merge_request.requester_username is null;
comment on column merge_request.reviewer_username is null;

alter table public.merge_request
  alter column requester_username drop not null,
  alter column requester_username drop default;
update public.merge_request mr
  set requester_username = null
  where requester_username = '';

insert into metadata.users(username, default_role)
    select requester_username, 'user' from public.merge_request
    where not requester_username = ''
  union distinct
    select reviewer_username, 'user' from public.merge_request
    where not reviewer_username = ''
  on conflict (username) do nothing;

alter table public.merge_request
  add constraint merge_request_requester_exists
    foreign key (requester_username)
    references metadata.users
    on update cascade
    on delete set null,
  add constraint merge_request_reviewer_exists
    foreign key (reviewer_username)
    references metadata.users
    on update cascade
    on delete set null;

comment on column merge_request.requester_username is e''
  'The user who created this merge request.';
comment on column merge_request.reviewer_username is e''
  'The user who reviews this merge request. Is empty until the request enters review.';

-- REMOVE SYSTEM USERS IF THEY WERE MISTAKENLY ADDED ABOVE
delete from metadata.users
where username = 'Aerie Legacy' or username = 'Mission Model';

-- UPDATE USERS ALLOWED ROLES
insert into metadata.users_allowed_roles(username, allowed_role)
  select u.username, roles.role
  from metadata.users u cross join (values ('user'), ('viewer')) roles(role);
-- ADD SYSTEM USERS
insert into metadata.users(username, default_role)
  values ('Mission Model', 'viewer'),
         ('Aerie Legacy', 'viewer');

call migrations.mark_migration_applied('19');
