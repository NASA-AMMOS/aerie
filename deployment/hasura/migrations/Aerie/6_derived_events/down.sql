-- up.sql creates table and sequence, delete them
DROP FUNCTION merlin.subtract_later_ranges CASCADE;
DROP TABLE merlin.derivation_group CASCADE;
-- derived_events view gets dropped automatically
-- derivation_group_comp view gets dropped automatically

call migrations.mark_migration_rolled_back('6');
