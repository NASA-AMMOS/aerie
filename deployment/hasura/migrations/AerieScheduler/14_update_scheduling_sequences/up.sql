SELECT setval(pg_get_serial_sequence('scheduling_goal_metadata', 'id'), coalesce(max(id),0) + 1, false) FROM scheduling_goal_metadata;
SELECT setval(pg_get_serial_sequence('scheduling_condition_metadata', 'id'), coalesce(max(id),0) + 1, false) FROM scheduling_condition_metadata;

call migrations.mark_migration_applied('14');
