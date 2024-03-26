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

-- Update Foreign Keys
alter table tags.scheduling_condition_tags
  add foreign key (tag_id) references tags.tags
    on update cascade
    on delete cascade;
alter table tags.scheduling_condition_definition_tags
  add foreign key (tag_id) references tags.tags
    on update cascade
    on delete cascade;
alter table tags.scheduling_goal_tags
  add foreign key (tag_id) references tags.tags
    on update cascade
    on delete cascade;
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

-- Add new constraints
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

alter table scheduler.scheduling_model_specification_conditions
add foreign key (model_id)
  references merlin.mission_model
  on update cascade
  on delete cascade;
alter table scheduler.scheduling_model_specification_goals
add foreign key (model_id)
    references merlin.mission_model
    on update cascade
    on delete cascade;
alter table scheduler.scheduling_specification
add constraint scheduling_spec_plan_id_fkey
    foreign key (plan_id)
    references merlin.plan
    on update cascade
    on delete cascade;
alter table scheduler.scheduling_condition_definition
add constraint condition_definition_author_exists
    foreign key (author)
    references permissions.users
    on update cascade
    on delete set null;
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
alter table scheduler.scheduling_goal_definition
add constraint goal_definition_author_exists
    foreign key (author)
    references permissions.users
    on update cascade
    on delete set null;
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
