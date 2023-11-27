create function notify_scheduling_workers_cancel()
returns trigger
security definer
language plpgsql as $$
begin
  perform pg_notify('scheduling_cancel', '' || new.specification_id);
  return null;
end
$$;

create trigger notify_scheduling_workers_cancel
after update of canceled on scheduling_request
for each row
when ((old.status != 'success' or old.status != 'failed') and new.canceled)
execute function notify_scheduling_workers_cancel();

call migrations.mark_migration_applied('12');
