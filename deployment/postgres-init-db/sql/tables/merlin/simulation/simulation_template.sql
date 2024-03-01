create table merlin.simulation_template (
  id integer generated always as identity,
  revision integer not null default 0,

  model_id integer not null,
  description text not null default '',
  arguments merlin.argument_set not null,
  owner text,

  constraint simulation_template_synthetic_key
    primary key (id),
  constraint simulation_template_owned_by_model
    foreign key (model_id)
    references merlin.mission_model
    on update cascade
    on delete cascade,
  constraint simulation_template_owner_exists
    foreign key (owner)
    references permissions.users
    on update cascade
    on delete set null
);

comment on table merlin.simulation_template is e''
  'A template specification for simulating an activity plan with a base set of arguments.';

comment on column merlin.simulation_template.id is e''
  'The synthetic identifier for this simulation template.';
comment on column merlin.simulation_template.revision is e''
  'A monotonic clock that ticks for every change to this simulation template.';
comment on column merlin.simulation_template.model_id is e''
  'The mission model used to obtain simulation configuration parameters.';
comment on column merlin.simulation_template.description is e''
  'A brief description to offer the planner information about the name or intent of this simulation template.';
comment on column merlin.simulation_template.arguments is e''
  'A subset of simulation arguments corresponding to the parameters of the associated mission model.';
comment on column merlin.simulation_template.owner is e''
  'The user responsible for this simulation template';

create trigger increment_revision_for_update_simulation_template_trigger
before update on merlin.simulation_template
for each row
when (pg_trigger_depth() < 1)
execute function util_functions.increment_revision_update();
