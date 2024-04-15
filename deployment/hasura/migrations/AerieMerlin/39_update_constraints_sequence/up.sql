SELECT setval(pg_get_serial_sequence('constraint_metadata', 'id'), coalesce(max(id),0) + 1, false) FROM constraint_metadata;

call migrations.mark_migration_applied('39');
