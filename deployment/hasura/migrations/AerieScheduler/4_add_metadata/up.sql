-- Scheduling Goal
alter table scheduling_goal
  add column tags text[] not null default '{}';
comment on column scheduling_goal.tags is e''
  'The tags associated with this scheduling goal.';

-- Scheduling Request
alter table scheduling_request
  add column requested_by text not null default '',
  add column requested_at timestamptz not null default now();

comment on column scheduling_request.requested_by is e''
  'The user who made the scheduling request.';
comment on column scheduling_request.requested_at is e''
  'When this scheduling request was made.';

call migrations.mark_migration_applied('4');
