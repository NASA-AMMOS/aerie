alter table scheduling_request
  add column dataset_id integer default null;

call migrations.mark_migration_applied('3');
