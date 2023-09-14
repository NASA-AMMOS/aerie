drop trigger cancel_pending_scheduling_rqs on scheduling_request;
drop function cancel_pending_scheduling_rqs();

call migrations.mark_migration_rolled_back('10');
