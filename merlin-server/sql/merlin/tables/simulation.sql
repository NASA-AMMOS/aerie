create table simulation (
  id integer generated always as identity,
  revision integer not null default 0,

  simulation_template_id integer null,
  plan_id integer not null,
  arguments merlin_argument_set not null,

  offset_from_plan_start interval default null,
  duration interval default null,

  constraint simulation_synthetic_key
    primary key (id),
  constraint simulation_has_simulation_template
    foreign key (simulation_template_id)
    references simulation_template
    on update cascade
    on delete set null,
  constraint simulation_owned_by_plan
    foreign key (plan_id)
    references plan
    on update cascade
    on delete cascade
);


comment on table simulation is e''
  'A specification for simulating an activity plan.';

comment on column simulation.id is e''
  'The synthetic identifier for this simulation.';
comment on column simulation.revision is e''
  'A monotonic clock that ticks for every change to this simulation.';
comment on column simulation.simulation_template_id is e''
  'A simulation template specification to inherit.';
comment on column simulation.plan_id is e''
  'The plan whose contents drive this simulation.';
comment on column simulation.arguments is e''
  'The set of arguments to this simulation, corresponding to the parameters of the associated mission model.';


create function increment_revision_for_update_simulation()
returns trigger
security definer
language plpgsql as $$begin
  update simulation
  set revision = revision + 1
  where id = new.id
    or id = old.id;

  return new;
end$$;

create trigger increment_revision_for_update_simulation_trigger
after update on simulation
for each row
when (pg_trigger_depth() < 1)
execute function increment_revision_for_update_simulation();
