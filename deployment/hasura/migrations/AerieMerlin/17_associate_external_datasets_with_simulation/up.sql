alter table plan_dataset
  add column simulation_dataset_id integer,
  add constraint associated_sim_dataset_exists
    foreign key (simulation_dataset_id)
    references simulation_dataset
    on update cascade
    on delete cascade;

comment on column plan_dataset.simulation_dataset_id is e''
  'The ID of the simulation dataset optionally associated with the dataset.'
  'If null, the dataset is associated with all simulation runs for the plan.';

call migrations.mark_migration_applied('17');
