drop trigger notify_scheduling_workers_cancel on scheduling_request;
drop function notify_scheduling_workers_cancel();
call migrations.mark_migration_rolled_back('12');
