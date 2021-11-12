create table if not exists simulation_dataset (
  simulation_id integer not null,
  dataset_id integer null,

  -- Determinant entities
  plan_revision integer not null,
  model_revision integer not null,
  simulation_revision integer not null,

  -- Dependent entities
  dataset_revision integer null,

  constraint simulation_dataset_primary_key
    primary key (dataset_id),
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

comment on table simulation_dataset is e''
  'A description of the upstream simulation inputs that determined a given dataset.';

comment on column simulation_dataset.simulation_id is e''
  'The simulation determining the contents of the associated dataset.';
comment on column simulation_dataset.dataset_id is e''
  'The dataset containing simulated results for the simulation.';
comment on column simulation_dataset.plan_revision is e''
  'The revision of the plan corresponding to the given revision of the dataset.';
comment on column simulation_dataset.model_revision is e''
  'The revision of the mission model corresponding to the given revision of the dataset.';
comment on column simulation_dataset.simulation_revision is e''
  'The revision of the simulation corresponding to the given revision of the dataset.';
comment on column simulation_dataset.dataset_revision is e''
  'The revision of the dataset corresponding to the given revisions of the input entities.';
