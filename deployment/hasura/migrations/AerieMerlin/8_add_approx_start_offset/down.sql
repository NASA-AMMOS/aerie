drop view activity_directive_extended;
drop function get_approximate_start_time(_activity_id int, _plan_id int);
call migrations.mark_migration_rolled_back('8');
