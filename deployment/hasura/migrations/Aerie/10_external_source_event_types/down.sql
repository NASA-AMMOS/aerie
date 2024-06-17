DROP TABLE merlin.external_source_event_types CASCADE;

call migrations.mark_migration_rolled_back('10');
