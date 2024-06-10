-- up.sql creates table and sequence, delete them
DROP TABLE merlin.plan_external_source CASCADE;

call migrations.mark_migration_rolled_back('7');
