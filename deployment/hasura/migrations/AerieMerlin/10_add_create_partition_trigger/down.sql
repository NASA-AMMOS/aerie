drop trigger create_partition_on_simulation on dataset;
drop function call_create_partition();
call migrations.mark_migration_rolled_back('10');
