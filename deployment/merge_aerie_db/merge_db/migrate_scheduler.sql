begin;
-- Move the contents of "public" to "scheduler"
alter schema public rename to scheduler;
comment on schema scheduler is 'Scheduler Service Schema';
create schema public;

-- Move Tags Tables
alter table metadata.scheduling_condition_tags set schema tags;
alter table metadata.scheduling_condition_definition_tags set schema tags;
alter table metadata.scheduling_goal_tags set schema tags;
alter table metadata.scheduling_goal_definition_tags set schema tags;

-- Metadata Schema is empty now
drop schema metadata;

-- Update function definitions
\! echo 'Migrating Scheduler Triggers...'
\ir migrate_scheduler_triggers.sql
\! echo 'Done!'

-- Add Tags Foreign Keys, removing orphan entries first
delete from tags.scheduling_condition_tags
  where not exists(
   select from tags.tags t
   where tag_id = t.id);
alter table tags.scheduling_condition_tags
  add foreign key (tag_id) references tags.tags
    on update cascade
    on delete cascade;

delete from tags.scheduling_condition_definition_tags
  where not exists(
   select from tags.tags t
   where tag_id = t.id);
alter table tags.scheduling_condition_definition_tags
  add foreign key (tag_id) references tags.tags
    on update cascade
    on delete cascade;

delete from tags.scheduling_goal_tags
  where not exists(
   select from tags.tags t
   where tag_id = t.id);
alter table tags.scheduling_goal_tags
  add foreign key (tag_id) references tags.tags
    on update cascade
    on delete cascade;

delete from tags.scheduling_goal_definition_tags
  where not exists(
   select from tags.tags t
   where tag_id = t.id);
alter table tags.scheduling_goal_definition_tags
  add foreign key (tag_id) references tags.tags
    on update cascade
    on delete cascade;

-- Replace status_t with util_functions.request_status
drop trigger notify_scheduling_workers_cancel on scheduler.scheduling_request;
alter table scheduler.scheduling_request
alter column status drop default,
alter column status type util_functions.request_status using status::text::util_functions.request_status,
alter column status set default 'pending';

create trigger notify_scheduling_workers_cancel
after update of canceled on scheduler.scheduling_request
for each row
when ((old.status != 'success' or old.status != 'failed') and new.canceled)
execute function scheduler.notify_scheduling_workers_cancel();

drop type scheduler.status_t;

-- Add new constraints, handling orphans first
update scheduler.scheduling_request
  set dataset_id = null
  where not exists(
   select from merlin.dataset d
   where dataset_id = d.id);
update scheduler.scheduling_request
  set requested_by = null
  where not exists(
   select from permissions.users u
   where requested_by = u.username);
alter table scheduler.scheduling_request
add constraint scheduling_request_requester_exists
  foreign key (requested_by)
    references permissions.users
    on update cascade
    on delete set null,
add constraint scheduling_request_references_dataset
  foreign key (dataset_id)
    references merlin.dataset
    on update cascade
    on delete set null;

delete from scheduler.scheduling_model_specification_conditions
  where not exists(
   select from merlin.mission_model m
   where model_id = m.id);
alter table scheduler.scheduling_model_specification_conditions
add foreign key (model_id)
  references merlin.mission_model
  on update cascade
  on delete cascade;

delete from scheduler.scheduling_model_specification_goals
  where not exists(
   select from merlin.mission_model m
   where model_id = m.id);
alter table scheduler.scheduling_model_specification_goals
add foreign key (model_id)
    references merlin.mission_model
    on update cascade
    on delete cascade;

delete from scheduler.scheduling_specification
  where not exists(
   select from merlin.plan p
   where plan_id = p.id);
alter table scheduler.scheduling_specification
add constraint scheduling_spec_plan_id_fkey
    foreign key (plan_id)
    references merlin.plan
    on update cascade
    on delete cascade;

update scheduler.scheduling_condition_definition
  set author = null
  where not exists(
   select from permissions.users u
   where author = u.username);
alter table scheduler.scheduling_condition_definition
add constraint condition_definition_author_exists
    foreign key (author)
    references permissions.users
    on update cascade
    on delete set null;

update scheduler.scheduling_condition_metadata
  set owner = null
  where not exists(
   select from permissions.users u
   where owner = u.username);
update scheduler.scheduling_condition_metadata
  set updated_by = null
  where not exists(
   select from permissions.users u
   where updated_by = u.username);
alter table scheduler.scheduling_condition_metadata
add constraint condition_owner_exists
  foreign key (owner)
  references permissions.users
    on update cascade
    on delete set null,
add constraint condition_updated_by_exists
  foreign key (updated_by)
  references permissions.users
    on update cascade
    on delete set null;

update scheduler.scheduling_goal_definition
  set author = null
  where not exists(
   select from permissions.users u
   where author = u.username);
alter table scheduler.scheduling_goal_definition
add constraint goal_definition_author_exists
    foreign key (author)
    references permissions.users
    on update cascade
    on delete set null;

update scheduler.scheduling_goal_metadata
  set owner = null
  where not exists(
   select from permissions.users u
   where owner = u.username);
update scheduler.scheduling_goal_metadata
  set updated_by = null
  where not exists(
   select from permissions.users u
   where updated_by = u.username);
alter table scheduler.scheduling_goal_metadata
add constraint goal_owner_exists
  foreign key (owner)
  references permissions.users
    on update cascade
    on delete set null,
add constraint goal_updated_by_exists
  foreign key (updated_by)
  references permissions.users
    on update cascade
    on delete set null;


-- Update function definitions
\! echo 'Migrating Scheduler Functions...'
\ir migrate_scheduler_functions.sql
\! echo 'Done!'
end;
