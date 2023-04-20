create table plan_dataset_to_simulation_dataset (
  plan_id integer not null,
  dataset_id integer not null,
  simulation_dataset_id integer not null,
  constraint plan_dataset_to_simulation_dataset_references_plan_dataset
    foreign key (plan_id, dataset_id)
      references plan_dataset
      on update cascade
      on delete cascade,
  constraint plan_dataset_to_simulation_dataset_references_sim_dataset
    foreign key (simulation_dataset_id)
      references simulation_dataset
      on update cascade
      on delete cascade,
  constraint plan_dataset_to_simulation_dataset_unique_sim_dataset
    unique (simulation_dataset_id)
);

alter table plan_dataset add column associated_simulation_dataset_id integer default null;

call migrations.mark_migration_applied('12');
