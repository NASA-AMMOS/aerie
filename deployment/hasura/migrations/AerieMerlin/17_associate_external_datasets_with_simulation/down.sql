alter table plan_dataset
  drop constraint associated_sim_dataset_exists,
  drop column simulation_dataset_id;

call migrations.mark_migration_rolled_back('17');
