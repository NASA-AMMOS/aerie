delete from constraint_run;

call migrations.mark_migration_rolled_back('25');
