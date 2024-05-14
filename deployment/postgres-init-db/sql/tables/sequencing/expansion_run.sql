create table sequencing.expansion_run (
  id integer generated always as identity,

  simulation_dataset_id integer not null,
  expansion_set_id integer not null,

  created_at timestamptz not null default now(),

  constraint expansion_run_primary_key
    primary key (id),

  foreign key (expansion_set_id)
    references sequencing.expansion_set (id)
    on delete cascade,
  foreign key (simulation_dataset_id)
    references merlin.simulation_dataset
    on delete cascade
);
comment on table sequencing.expansion_run is e''
  'The configuration for an expansion run for a plan.';
comment on column sequencing.expansion_run.id is e''
  'The synthetic identifier for this expansion run.';
comment on column sequencing.expansion_run.simulation_dataset_id is e''
  'The simulation dataset id used to generate this expansion run.';
comment on column sequencing.expansion_run.expansion_set_id is e''
  'The command dictionary, mission model, and expansion set id.';
