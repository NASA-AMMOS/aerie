drop trigger set_timestamp on merge_request;
drop function merge_request_set_updated_at();
alter table merge_request drop column updated_at;

call migrations.mark_migration_rolled_back('36');
