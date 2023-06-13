create table scheduling_condition (
  id integer generated always as identity,
  revision integer not null default 0,
  name text not null,
  definition text not null,

  model_id integer not null,
  description text not null default '',
  author integer,
  last_modified_by integer,
  created_date timestamptz not null default now(),
  modified_date timestamptz not null default now(),

  constraint scheduling_condition_synthetic_key
    primary key (id)
);

comment on table scheduling_condition is e''
  'A condition restricting scheduling of a plan.';
comment on column scheduling_condition.id is e''
  'The synthetic identifier for this scheduling condition.';
comment on column scheduling_condition.revision is e''
  'A monotonic clock that ticks for every change to this scheduling condition.';
comment on column scheduling_condition.definition is e''
  'The source code for a Typescript module defining this scheduling condition';
comment on column scheduling_condition.model_id is e''
  'The mission model used to which this scheduling condition is associated.';
comment on column scheduling_condition.name is e''
  'A short human readable name for this condition';
comment on column scheduling_condition.description is e''
  'A longer text description of this scheduling condition.';
comment on column scheduling_condition.author is e''
  'The original user who authored this scheduling condition.';
comment on column scheduling_condition.last_modified_by is e''
  'The last user who modified this scheduling condition.';
comment on column scheduling_condition.created_date is e''
  'The date this scheduling condition was created.';
comment on column scheduling_condition.modified_date is e''
  'The date this scheduling condition was last modified.';

create function update_logging_on_update_scheduling_condition()
  returns trigger
  security definer
language plpgsql as $$begin
  new.revision = old.revision + 1;
  new.modified_date = now();
return new;
end$$;

create trigger update_logging_on_update_scheduling_condition_trigger
  before update on scheduling_condition
  for each row
  when (pg_trigger_depth() < 1)
  execute function update_logging_on_update_scheduling_condition();
