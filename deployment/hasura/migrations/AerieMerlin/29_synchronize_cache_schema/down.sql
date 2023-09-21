-- The JSON schema of the constraint violations in constraint_run is changed.
-- Because the table is a cache it is safer to clear it than to attempt to transform it using SQL
truncate constraint_run;
call migrations.mark_migration_rolled_back('29');
