create table simulation_template (
  id integer generated always as identity,
  revision integer not null default 0,

  model_id integer not null,
  description text not null,
  arguments merlin_argument_set not null,

  simulation_start_time timestamptz default null,
  simulation_end_time timestamptz default null,

  constraint simulation_template_synthetic_key
    primary key (id),
  constraint simulation_template_owned_by_model
    foreign key (model_id)
    references mission_model
    on update cascade
    on delete cascade
);

comment on table simulation_template is e''
  'A template specification for simulating an activity plan with a base set of arguments.';

comment on column simulation_template.id is e''
  'The synthetic identifier for this simulation template.';
comment on column simulation_template.revision is e''
  'A monotonic clock that ticks for every change to this simulation template.';
comment on column simulation_template.model_id is e''
  'The mission model used to obtain simulation configuration parameters.';
comment on column simulation_template.description is e''
  'A brief description to offer the planner information about the name or intent of this simulation template.';
comment on column simulation_template.arguments is e''
  'A subset of simulation arguments corresponding to the parameters of the associated mission model.';


create function increment_revision_for_update_simulation_template()
returns trigger
security definer
language plpgsql as $$begin
  update simulation_template
  set revision = revision + 1
  where id = new.id
    or id = old.id;

  return new;
end$$;

create trigger increment_revision_for_update_simulation_template_trigger
after update on simulation_template
for each row
when (pg_trigger_depth() < 1)
execute function increment_revision_for_update_simulation_template();
