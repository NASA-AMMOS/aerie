-- Drop Triggers
drop trigger notify_scheduler_workers on scheduling_request;

alter table scheduling_request
  add column for_aerie_scheduler boolean not null default true;

comment on column scheduling_request.for_aerie_scheduler is e''
  'Whether the request is destined to the aerie scheduler or to an external scheduler;';

create trigger notify_scheduler_workers
  after insert on scheduling_request
  for each row
  when (NEW.for_aerie_scheduler)
  execute function notify_scheduler_workers();

call migrations.mark_migration_applied('8');
