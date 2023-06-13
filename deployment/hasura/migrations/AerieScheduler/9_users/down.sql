-- Scheduling Request
comment on column scheduling_request.requested_by is null;

alter table scheduling_request
  drop column requested_by;
alter table scheduling_request
  add column requested_by text not null default '';

comment on column scheduling_request.requested_by is e''
  'The user who made the scheduling request.';

-- Scheduling Goal
comment on column scheduling_goal.last_modified_by is null;
comment on column scheduling_goal.author is null;

alter table scheduling_goal
  drop column author,
  drop column last_modified_by;
alter table scheduling_goal
  add column author text null,
  add column last_modified_by text null;

comment on column scheduling_goal.author is e''
  'The original user who authored this scheduling goal.';
comment on column scheduling_goal.last_modified_by is e''
  'The last user who modified this scheduling goal.';

-- Scheduling Condition
comment on column scheduling_condition.last_modified_by is null;
comment on column scheduling_condition.author is null;

alter table scheduling_condition
  drop column author,
  drop column last_modified_by;
alter table scheduling_condition
  add column author text null,
  add column last_modified_by text null;

comment on column scheduling_condition.author is e''
  'The original user who authored this scheduling condition.';
comment on column scheduling_condition.last_modified_by is e''
  'The last user who modified this scheduling condition.';

call migrations.mark_migration_rolled_back('9');
