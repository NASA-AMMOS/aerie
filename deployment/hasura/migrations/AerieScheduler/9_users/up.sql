-- Scheduling Request
alter table scheduling_request
  alter column requested_by drop not null,
  alter column requested_by drop default;
update scheduling_request
  set requested_by = null
  where requested_by = '';

call migrations.mark_migration_applied('9');
