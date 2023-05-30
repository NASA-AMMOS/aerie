create type status_t as enum('pending', 'incomplete', 'failed', 'success');

create table constraint_run (
  constraint_id integer not null,
  constraint_definition text not null,
  simulation_id integer not null,

  status status_t not null default 'pending',
  violations jsonb null,

  -- Additional Metadata
  requested_by text not null default '',
  requested_at timestamptz not null default now(),

  constraint constraint_run_primary_key
    primary key (id),
  constraint constraint_run_to_constraint
    foreign key (constraint_id)
    references constraint
    on delete cascade,
  constraint constraint_run_to_simulation
    foreign key (simulation_id)
    references simulation
    on delete cascade
);

comment on table constraint_run is e''
  '';

comment on column constraint_run.constraint_id is e''
  'The constraint that we are evaluating during the run.';
comment on column constraint_run.constraint_definition is e''
  'The definition of the constraint that is being checked, used to determine staleness.';
comment on column constraint_run.simulation_id is e''
  'The simulation id from when the constraint was checked, used to determine staleness.';
comment on column constraint_run.status is e''
  'The current status of the constraint run.';
comment on column constraint_run.violations is e''
  'Any violations that were found during the constraint check.';
comment on column constraint_run.requested_by is e''
  'The user who requested the constraint run.';
comment on column constraint_run.requested_at is e''
  'When the constraint run was created.';
