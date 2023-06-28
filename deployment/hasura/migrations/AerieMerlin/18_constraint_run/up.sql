create type constraint_status as enum('resolved', 'constraint-outdated', 'simulation-outdated');

create table constraint_run (
  constraint_id integer not null,
  constraint_definition text not null,
  dataset_id integer not null,

  status constraint_status not null default 'resolved',
  violations jsonb null,

  -- Additional Metadata
  requested_by text not null default '',
  requested_at timestamptz not null default now(),

  constraint constraint_run_to_constraint
    foreign key (constraint_id)
      references "constraint"
      on delete cascade,
  constraint constraint_run_to_simulation_dataset
    foreign key (dataset_id)
      references simulation_dataset
      on delete cascade
);

comment on table constraint_run is e''
  'A single constraint run, used to cache violation results to be reused if the constraint and simulation are not stale.';

comment on column constraint_run.constraint_id is e''
  'The constraint that we are evaluating during the run.';
comment on column constraint_run.constraint_definition is e''
  'The definition of the constraint that is being checked, used to determine staleness.';
comment on column constraint_run.dataset_id is e''
  'The simulation dataset id from when the constraint was checked, used to determine staleness.';
comment on column constraint_run.status is e''
  'The current status of the constraint run.';
comment on column constraint_run.violations is e''
  'Any violations that were found during the constraint check.';
comment on column constraint_run.requested_by is e''
  'The user who requested the constraint run.';
comment on column constraint_run.requested_at is e''
  'When the constraint run was created.';

create or replace function constraint_check_constraint_run()
  returns trigger
  security definer
  language plpgsql as $$begin
  update constraint_run
  set status = 'constraint-outdated'
  where constraint_id = new.id and constraint_definition != new.definition;
  return new;
end$$;

create trigger constraint_check_constraint_run_trigger
  after update on "constraint"
  for each row
execute function constraint_check_constraint_run();

create or replace function simulation_dataset_check_constraint_run()
  returns trigger
  security definer
  language plpgsql as $$begin
  if new.simulation_id = old.simulation_id
  then
    update constraint_run
    set status = 'simulation-outdated'
    where old.dataset_id = dataset_id;
  end if;
  return new;
end$$;

create trigger simulation_dataset_check_constraint_run_trigger
  before insert on simulation_dataset
  for each row
execute function simulation_dataset_check_constraint_run();

call migrations.mark_migration_applied('18');
