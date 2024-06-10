-- up.sql creates table and sequence, delete here
DROP TABLE merlin.external_source_type CASCADE;

call migrations.mark_migration_rolled_back('6');