-- The schema of the database is unchanged, but the JSON schema of the constraint violations in constraint_run is different.
-- Theoretically we could use SQL to transform the schemas of existing JSON objects, but since the table is just a cache it is safe to clear it
-- programming is hard, ok
delete from constraint_run;

call migrations.mark_migration_applied('25');
