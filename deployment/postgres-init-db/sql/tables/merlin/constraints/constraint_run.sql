create table merlin.constraint_run (
  constraint_id integer not null,
  constraint_revision integer not null,
  simulation_dataset_id integer not null,
  constraint_invocation_id integer not null,
  arguments jsonb not null,

  results jsonb not null default '{}',

  -- Additional Metadata
  requested_by text,
  requested_at timestamptz not null default now(),

  constraint constraint_run_key
    primary key (constraint_invocation_id, arguments, simulation_dataset_id),
  constraint constraint_run_to_constraint_definition
    foreign key (constraint_id, constraint_revision)
      references merlin.constraint_definition
      on delete cascade,
  constraint constraint_run_to_simulation_dataset
    foreign key (simulation_dataset_id)
      references merlin.simulation_dataset
      on delete cascade,
  constraint constraint_run_requested_by
    foreign key (requested_by)
      references permissions.users
      on update cascade
      on delete set null
);

create index constraint_run_simulation_dataset_id_index
  on merlin.constraint_run (simulation_dataset_id);

comment on table merlin.constraint_run is e''
  'A single constraint run, used to cache violation results to be reused if the constraint definition is not stale.';

comment on column merlin.constraint_run.constraint_id is e''
  'The constraint that we are evaluating during the run.';
comment on column merlin.constraint_run.constraint_revision is e''
  'The version of the constraint definition that was checked.';
comment on column merlin.constraint_run.simulation_dataset_id is e''
  'The simulation dataset id from when the constraint was checked.';
comment on column merlin.constraint_run.results is e''
  'Results that were computed during the constraint check.';
comment on column merlin.constraint_run.requested_by is e''
  'The user who requested the constraint run.';
comment on column merlin.constraint_run.requested_at is e''
  'When the constraint run was created.';
