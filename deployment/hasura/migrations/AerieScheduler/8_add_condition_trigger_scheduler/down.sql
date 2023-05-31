-- Drop Triggers
drop trigger notify_scheduler_workers on scheduling_request;

alter table scheduling_request
  drop column for_aerie_scheduler;

comment on column scheduling_goal.for_aerie_scheduler is null;

create trigger notify_scheduler_workers
  after insert on scheduling_request
  for each row
  execute function notify_scheduler_workers();

call migrations.mark_migration_rolled_back('8');
