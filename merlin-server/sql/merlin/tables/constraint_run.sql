create table constraint_run (
  constraint_id integer not null,
  constraint_definition text not null,
  simulation_dataset_id integer not null,

  definition_outdated boolean default false not null,
  violations jsonb not null default '{}',

  -- Additional Metadata
  requested_by text,
  requested_at timestamptz not null default now(),

  constraint constraint_run_key
    primary key (constraint_id, constraint_definition, simulation_dataset_id),
  constraint constraint_run_to_constraint
    foreign key (constraint_id)
      references "constraint"
      on delete cascade,
  constraint constraint_run_to_simulation_dataset
    foreign key (simulation_dataset_id)
      references simulation_dataset
      on delete cascade,
  constraint constraint_run_requested_by
    foreign key (requested_by)
      references metadata.users
      on update cascade
      on delete set null
);

create index constraint_run_simulation_dataset_id_index
  on constraint_run (simulation_dataset_id);

comment on table constraint_run is e''
  'A single constraint run, used to cache violation results to be reused if the constraint definition is not stale.';

comment on column constraint_run.constraint_id is e''
  'The constraint that we are evaluating during the run.';
comment on column constraint_run.constraint_definition is e''
  'The definition of the constraint when it was checked, used to determine staleness.';
comment on column constraint_run.simulation_dataset_id is e''
  'The simulation dataset id from when the constraint was checked.';
comment on column constraint_run.definition_outdated is e''
  'Tracks if the constraint definition is outdated because the constraint has been changed.';
comment on column constraint_run.violations is e''
  'Any violations that were found during the constraint check.';
comment on column constraint_run.requested_by is e''
  'The user who requested the constraint run.';
comment on column constraint_run.requested_at is e''
  'When the constraint run was created.';
