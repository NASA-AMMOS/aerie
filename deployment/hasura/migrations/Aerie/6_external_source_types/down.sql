-- up.sql creates table and sequence, delete here
DROP TABLE merlin.external_source_type CASCADE;

-- Drop source_type_id column from external_source
ALTER TABLE merlin.exteranl_type DROP COLUMN source_type_id;

call migrations.mark_migration_rolled_back('6');