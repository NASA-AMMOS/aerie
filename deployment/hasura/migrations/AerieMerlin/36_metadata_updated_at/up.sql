alter table merge_request add column updated_at timestamptz not null default now();

create function merge_request_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
  before update or insert on merge_request
  for each row
execute function merge_request_set_updated_at();

call migrations.mark_migration_applied('36');

