create table scheduling_template (
  id integer generated always as identity,
  revision integer not null default 0,

  model_id integer not null,
  description text not null,
  simulation_arguments jsonb not null,

  constraint scheduling_template_synthetic_key
    primary key (id)
);

comment on table scheduling_template is e''
  'A template scheduling spec from which scheduling requests may be based.'
'\n'
  'The associated scheduling goals are stored in the scheduling_template_goals table.';
comment on column scheduling_template.id is e''
  'The synthetic identifier for this scheduling template.';
comment on column scheduling_template.revision is e''
  'A monotonic clock that ticks for every change to this scheduling template.';
comment on column scheduling_template.model_id is e''
  'The mission model used to which this scheduling template is associated.';
comment on column scheduling_template.description is e''
  'A text description of this scheduling template.';
comment on column scheduling_template.simulation_arguments is e''
  'A set of simulation arguments to be used for simulation during scheduling.';

create function increment_revision_on_update_scheduling_template()
  returns trigger
  security definer
language plpgsql as $$begin
  new.revision = old.revision + 1;
return new;
end$$;

create trigger increment_revision_on_update_scheduling_template_trigger
  before update on scheduling_template
  for each row
  when (pg_trigger_depth() < 1)
  execute function increment_revision_on_update_scheduling_template();
