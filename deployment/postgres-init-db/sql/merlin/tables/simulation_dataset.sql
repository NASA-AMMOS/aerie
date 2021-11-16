create table simulation_dataset (
  simulation_id integer not null,
  dataset_id integer null,

  -- Determinant entities
  plan_revision integer not null,
  model_revision integer not null,
  simulation_template_revision integer null,
  simulation_revision integer not null,

  -- Dependent entities
  dataset_revision integer null,

  -- Simulation state
  state text not null,
  reason text null,
  canceled boolean not null,
  offset_from_plan_start interval not null,

  constraint simulation_dataset_dataset_has_a_simulation
    unique (dataset_id),
  constraint simulation_dataset_references_simulation
    foreign key (simulation_id)
    references simulation
    on update cascade
    on delete cascade,
  constraint simulation_dataset_references_dataset
    foreign key (dataset_id)
    references dataset
    on update cascade
    on delete cascade
);

create index simulation_dataset_simulation_has_many_datasets
  on simulation_dataset (simulation_id);

comment on table simulation_dataset is e''
  'A description of the upstream simulation inputs that determined a given dataset.';

comment on column simulation_dataset.simulation_id is e''
  'The simulation determining the contents of the associated dataset.';
comment on column simulation_dataset.dataset_id is e''
  'The dataset containing simulated results for the simulation. NULL if the dataset has not been constructed yet.';
comment on column simulation_dataset.plan_revision is e''
  'The revision of the plan corresponding to the given revision of the dataset.';
comment on column simulation_dataset.model_revision is e''
  'The revision of the mission model corresponding to the given revision of the dataset.';
comment on column simulation_dataset.simulation_revision is e''
  'The revision of the simulation corresponding to the given revision of the dataset.';
comment on column simulation_dataset.dataset_revision is e''
  'The revision of the dataset corresponding to the given revisions of the input entities.';
comment on column simulation_dataset.state is e''
  'The state of the simulation for which the dataset is associated.';
comment on column simulation_dataset.reason is e''
  'The reason for failure in the event that simulation fails.';
comment on column simulation_dataset.canceled is e''
  'Whether the simulation has been marked as canceled.';
comment on column simulation_dataset.offset_from_plan_start is e''
  'The time to judge dataset items against relative to the plan start.'
'\n'
  'If the dataset as a whole begins one day before the planning period begins, '
  'then this column should contain the interval ''1 day ago''.';

create or replace function create_dataset_on_insert()
returns trigger
security definer
language plpgsql as $$begin
  insert into dataset
  default values
  returning id into new.dataset_id;
return new;
end$$;

create or replace function delete_dataset_on_delete()
returns trigger
security definer
language plpgsql as $$begin
  delete from dataset
  where id = old.dataset_id;
return old;
end$$;

do $$ begin
create trigger create_dataset_on_insert_trigger
  before insert on simulation_dataset
  for each row
  execute function create_dataset_on_insert();
exception
  when duplicate_object then null;
end $$;

do $$ begin
create trigger delete_dataset_on_delete_trigger
  before delete on simulation_dataset
  for each row
  execute function delete_dataset_on_delete();
exception
  when duplicate_object then null;
end $$;
