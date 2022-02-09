create table scheduling_rule (
  id integer generated always as identity,
  revision integer not null default 0,
  definition jsonb not null,

  model_id integer not null,
  description text null,
  author text null,
  last_modified_by text null,
  created_date timestamptz not null default now(),
  modified_date timestamptz not null default now(),

  constraint scheduling_rule_synthetic_key
    primary key (id)
);

comment on table scheduling_rule is e''
  'A rule or rule for scheduling of a plan.';
comment on column scheduling_rule.id is e''
  'The synthetic identifier for this scheduling rule.';
comment on column scheduling_rule.revision is e''
  'A monotonic clock that ticks for every change to this scheduling rule.';
comment on column scheduling_rule.definition is e''
  'A JSON representation of this rule''s structure';
comment on column scheduling_rule.model_id is e''
  'The mission model used to which this scheduling rule is associated.';
comment on column scheduling_rule.description is e''
  'A text description of this scheduling rule.';
comment on column scheduling_rule.author is e''
  'The original user who authored this scheduling rule.';
comment on column scheduling_rule.last_modified_by is e''
  'The last user who modified this scheduling rule.';
comment on column scheduling_rule.created_date is e''
  'The date this scheduling rule was created.';
comment on column scheduling_rule.modified_date is e''
  'The date this scheduling rule was last modified.';

create function update_logging_on_update_scheduling_rule()
  returns trigger
  security definer
language plpgsql as $$begin
  new.revision = old.revision + 1;
  new.modified_date = now();
return new;
end$$;

create trigger update_logging_on_update_scheduling_rule_trigger
  before update on scheduling_rule
  for each row
  when (pg_trigger_depth() < 1)
  execute function update_logging_on_update_scheduling_rule();
