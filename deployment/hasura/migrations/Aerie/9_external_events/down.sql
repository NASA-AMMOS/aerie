-- up.sql creates table and sequence, delete them
DROP TABLE merlin.external_source CASCADE;
DROP TABLE merlin.external_event CASCADE;
DROP TABLE merlin.plan_derivation_group CASCADE;
DROP TABLE merlin.external_source_type CASCADE;
DROP TABLE merlin.external_event_type CASCADE;

call migrations.mark_migration_rolled_back('9');
