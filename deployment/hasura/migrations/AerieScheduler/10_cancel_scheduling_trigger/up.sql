create function cancel_pending_scheduling_rqs()
returns trigger
security definer
language plpgsql as $$
begin
  update scheduling_request
  set canceled = true
  where status = 'pending'
  and specification_id = new.specification_id;
  return new;
end
$$;

create trigger cancel_pending_scheduling_rqs
  before insert on scheduling_request
  for each row
  execute function cancel_pending_scheduling_rqs();

call migrations.mark_migration_applied('10');
