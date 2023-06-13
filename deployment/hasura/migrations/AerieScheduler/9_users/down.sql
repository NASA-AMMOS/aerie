-- Scheduling Request
alter table scheduling_request
  alter column requested_by set default '';
update scheduling_request
  set requested_by = default
  where requested_by is null;
alter table scheduling_request
  alter column requested_by set not null;

call migrations.mark_migration_rolled_back('9');
