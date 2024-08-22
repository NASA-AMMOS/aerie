-- up.sql creates table and sequence, delete them
DROP TABLE ui.seen_sources CASCADE;
DROP TABLE merlin.plan_derivation_group CASCADE;
DROP TABLE merlin.external_event CASCADE;
DROP TABLE merlin.external_source CASCADE;
DROP TABLE merlin.external_source_type CASCADE;
DROP TABLE merlin.external_event_type CASCADE;
DROP TABLE merlin.derivation_group CASCADE;
DROP FUNCTION merlin.subtract_later_ranges CASCADE;

call migrations.mark_migration_rolled_back('9');
