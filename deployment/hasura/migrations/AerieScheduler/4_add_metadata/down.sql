-- Scheduling Request
comment on column scheduling_request.requested_at is null;
comment on column scheduling_request.requested_by is null;

alter table scheduling_request
  drop column requested_at,
  drop column requested_by;

-- Scheduling Goal
comment on column scheduling_goal.tags is null;
alter table scheduling_goal
  drop column tags;

call migrations.mark_migration_rolled_back('4');
