alter table scheduling_request
  drop column dataset_id;

call migrations.mark_migration_rolled_back('3');
