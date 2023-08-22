-- The schema of the database is unchanged, but the JSON schema of the constraint violations in constraint_run is different.
-- Theoretically we could use SQL to transform the schemas of existing JSON objects, but I'd rather just delete them.
-- programming is hard, ok
delete from constraint_run;

call migrations.mark_migration_applied('25');
