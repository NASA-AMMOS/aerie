create table expansion_run (
  id integer generated always as identity,

  simulation_id integer not null,
  expansion_set_id integer not null,

  constraint expansion_run_primary_key
    primary key (id),

  foreign key (expansion_set_id)
  references expansion_set (id)
);
comment on table expansion_run is e''
  'The configuration for an expansion run for a plan.';
comment on column expansion_run.id is e''
  'The synthetic identifier for this expansion run.';
comment on column expansion_run.simulation_id is e''
  'The simulation results for the plan.';
comment on column expansion_run.expansion_set_id is e''
  'The command dictionary and mission model set';
