begin;
-- Create Cross Service Schemas (migrations already exists and hasura will be created via rename)
comment on schema migrations is 'DB Migrations Schema';
create schema permissions;
comment on schema permissions is 'Aerie User and User Roles Schema';
create schema tags;
comment on schema tags is 'Tags Metadata Schema';
create schema util_functions;
comment on schema util_functions is 'Cross-service Helper Function Schema';

-- Drop the PGCrypto extension that Hasura auto-installed out of "public"
drop extension if exists pgcrypto;

-- Move the contents of "public" to "merlin"
alter schema public rename to merlin;
comment on schema merlin is 'Merlin Service Schema';
create schema public;

-- Move the contents of "hasura_functions" to "hasura"
alter schema hasura_functions rename to hasura;
comment on schema hasura is 'Hasura Helper Function Schema';
alter type merlin.resolution_type set schema hasura;

-- Empty schema_migrations
truncate migrations.schema_migrations;
call migrations.mark_migration_applied('0');

-- Move Permissions Tables
alter table metadata.users set schema permissions;
alter table metadata.users_allowed_roles set schema permissions;
alter table metadata.users_and_roles set schema permissions;
alter table metadata.user_role_permission set schema permissions;
alter table metadata.user_roles set schema permissions;

-- Move Permissions Types
alter type metadata.permission set schema permissions;
alter type metadata.action_permission_key set schema permissions;
alter type metadata.function_permission_key set schema permissions;

-- Move Tags Tables
alter table metadata.tags set schema tags;
alter table metadata.plan_tags set schema tags;
alter table metadata.plan_snapshot_tags set schema tags;
alter table metadata.activity_directive_tags set schema tags;
alter table metadata.snapshot_activity_tags set schema tags;
alter table metadata.constraint_definition_tags set schema tags;
alter table metadata.constraint_tags set schema tags;

-- Move Permissions Functions
alter procedure metadata.check_general_permissions(_function permissions.function_permission_key, _permission permissions.permission, _plan_id integer, _user text) set schema permissions;
alter procedure metadata.check_merge_permissions(_function permissions.function_permission_key, _merge_request_id integer, hasura_session json) set schema permissions;
alter procedure metadata.check_merge_permissions(_function permissions.function_permission_key, _permission permissions.permission, _plan_id_receiving integer, _plan_id_supplying integer, _user text) set schema permissions;
alter function metadata.get_function_permissions(_function permissions.function_permission_key, hasura_session json) set schema permissions;
alter function metadata.get_role(hasura_session json) set schema permissions;
alter function metadata.insert_permission_for_user_role() set schema permissions;
alter function metadata.raise_if_plan_merge_permission(_function permissions.function_permission_key, _permission permissions.permission) set schema permissions;
alter function metadata.validate_permissions_json() set schema permissions;

-- Move Tags Functions
alter function merlin.get_tags(_activity_id int, _plan_id int) set schema tags;
alter function merlin.adt_check_locked_new() set schema tags;
alter function merlin.adt_check_locked_old() set schema tags;
alter function merlin.snapshot_tags_in_review_delete() set schema tags;
alter function metadata.tag_ids_activity_directive(_directive_id integer, _plan_id integer) set schema tags;
alter function metadata.tag_ids_activity_snapshot(_directive_id integer, _snapshot_id integer) set schema tags;

-- Metadata Schema is empty now
drop schema metadata;

-- Replace status_t with util_functions.request_status
create type util_functions.request_status as enum('pending', 'incomplete', 'failed', 'success');

drop trigger notify_simulation_workers_cancel on merlin.simulation_dataset;
alter table merlin.simulation_dataset
alter column status drop default,
alter column status type util_functions.request_status using status::text::util_functions.request_status,
alter column status set default 'pending';

create trigger notify_simulation_workers_cancel
after update of canceled on merlin.simulation_dataset
for each row
when ((old.status != 'success' or old.status != 'failed') and new.canceled)
execute function merlin.notify_simulation_workers_cancel();

drop type merlin.status_t;

-- Update Tables
alter table merlin.activity_type
  rename constraint activity_type_owned_by_mission_model to activity_type_mission_model_exists;
alter table merlin.activity_type rename constraint activity_type_natural_key to activity_type_pkey;

-- Update Types
alter domain merlin.merlin_parameter_set rename to parameter_set;
alter domain merlin.merlin_argument_set rename to argument_set;
alter domain merlin.merlin_required_parameter_set rename to required_parameter_set;
alter type merlin.merlin_activity_directive_metadata_set rename to activity_directive_metadata_set;

-- Update function definitions
\! echo 'Migrating Merlin Functions...'
\ir ./migrate_merlin_functions.sql
\! echo 'Done!'
\! echo 'Migrating Hasura Functions...'
\ir ./migrate_hasura_functions.sql
\! echo 'Done!'
\! echo 'Migrating Tags Functions...'
\ir ./migrate_tags_functions.sql
\! echo 'Done!'
\! echo 'Migrating Permissions Functions...'
\ir ./migrate_permissions_functions.sql
\! echo 'Done!'

-- Update Views
-- Update Views
create or replace view merlin.activity_directive_extended as
(
  select
    -- Activity Directive Properties
    ad.id as id,
    ad.plan_id as plan_id,
    -- Additional Properties
    ad.name as name,
    tags.get_tags(ad.id, ad.plan_id) as tags,
    ad.source_scheduling_goal_id as source_scheduling_goal_id,
    ad.created_at as created_at,
    ad.created_by as created_by,
    ad.last_modified_at as last_modified_at,
    ad.last_modified_by as last_modified_by,
    ad.start_offset as start_offset,
    ad.type as type,
    ad.arguments as arguments,
    ad.last_modified_arguments_at as last_modified_arguments_at,
    ad.metadata as metadata,
    ad.anchor_id as anchor_id,
    ad.anchored_to_start as anchored_to_start,
    -- Derived Properties
    merlin.get_approximate_start_time(ad.id, ad.plan_id) as approximate_start_time,
    ptd.preset_id as preset_id,
    ap.arguments as preset_arguments
   from merlin.activity_directive ad
   left join merlin.preset_to_directive ptd on ad.id = ptd.activity_id and ad.plan_id = ptd.plan_id
   left join merlin.activity_presets ap on ptd.preset_id = ap.id
);

create or replace view merlin.simulated_activity as
(
  select span.id as id,
         sd.id as simulation_dataset_id,
         span.parent_id as parent_id,
         span.start_offset as start_offset,
         span.duration as duration,
         span.attributes as attributes,
         span.type as activity_type_name,
         (span.attributes#>>'{directiveId}')::integer as directive_id,
         sd.simulation_start_time + span.start_offset as start_time,
         sd.simulation_start_time + span.start_offset + span.duration as end_time
   from merlin.span span
     join merlin.dataset d on span.dataset_id = d.id
     join merlin.simulation_dataset sd on d.id = sd.dataset_id
     join merlin.simulation s on s.id = sd.simulation_id
);
end;
