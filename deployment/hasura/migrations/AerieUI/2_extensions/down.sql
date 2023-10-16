drop table extension_roles;

drop trigger extensions_set_timestamp on extensions;
drop function extensions_set_updated_at();

drop table extensions;

call migrations.mark_migration_rolled_back('2');
