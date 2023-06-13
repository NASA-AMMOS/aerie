-- Scheduling Condition
comment on column scheduling_condition.last_modified_by is null;
comment on column scheduling_condition.author is null;

alter table scheduling_condition
  drop column author,
  drop column last_modified_by;
alter table scheduling_condition
  add column author integer,
  add column last_modified_by integer;

comment on column scheduling_condition.author is e''
  'The original user who authored this scheduling condition.';
comment on column scheduling_condition.last_modified_by is e''
  'The last user who modified this scheduling condition.';

-- Scheduling Goal
comment on column scheduling_goal.last_modified_by is null;
comment on column scheduling_goal.author is null;

alter table scheduling_goal
  drop column author,
  drop column last_modified_by;
alter table scheduling_goal
  add column author integer,
  add column last_modified_by integer;

comment on column scheduling_goal.author is e''
  'The original user who authored this scheduling goal.';
comment on column scheduling_goal.last_modified_by is e''
  'The last user who modified this scheduling goal.';

-- Scheduling Request
comment on column scheduling_request.requested_by is null;

alter table scheduling_request
  drop column requested_by;
alter table scheduling_request
  add column requested_by integer;

comment on column scheduling_request.requested_by is e''
  'The user who made the scheduling request.';

call migrations.mark_migration_applied('9');
