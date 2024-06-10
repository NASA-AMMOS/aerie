-- up.sql creates table and sequence, delete them
DROP TABLE merlin.external_event CASCADE;

call migrations.mark_migration_rolled_back('6');
