create table scheduling_goal (
  id integer generated always as identity,
  revision integer not null default 0,
  name text not null,
  definition text not null,
  tags text[] not null default '{}',

  model_id integer not null,
  description text null,
  author text null,
  last_modified_by text null,
  created_date timestamptz not null default now(),
  modified_date timestamptz not null default now(),

  constraint scheduling_goal_synthetic_key
    primary key (id)
);

comment on table scheduling_goal is e''
  'A goal for scheduling of a plan.';
comment on column scheduling_goal.id is e''
  'The synthetic identifier for this scheduling goal.';
comment on column scheduling_goal.revision is e''
  'A monotonic clock that ticks for every change to this scheduling goal.';
comment on column scheduling_goal.definition is e''
  'The source code for a Typescript module defining this scheduling goal';
comment on column scheduling_goal.model_id is e''
  'The mission model used to which this scheduling goal is associated.';
comment on column scheduling_goal.name is e''
  'A short human readable name for this goal';
comment on column scheduling_goal.description is e''
  'A longer text description of this scheduling goal.';
comment on column scheduling_goal.author is e''
  'The original user who authored this scheduling goal.';
comment on column scheduling_goal.last_modified_by is e''
  'The last user who modified this scheduling goal.';
comment on column scheduling_goal.created_date is e''
  'The date this scheduling goal was created.';
comment on column scheduling_goal.modified_date is e''
  'The date this scheduling goal was last modified.';
comment on column scheduling_goal.tags is e''
  'The tags associated with this scheduling goal.';

create function update_logging_on_update_scheduling_goal()
  returns trigger
  security definer
language plpgsql as $$begin
  new.revision = old.revision + 1;
  new.modified_date = now();
return new;
end$$;

create trigger update_logging_on_update_scheduling_goal_trigger
  before update on scheduling_goal
  for each row
  when (pg_trigger_depth() < 1)
  execute function update_logging_on_update_scheduling_goal();
