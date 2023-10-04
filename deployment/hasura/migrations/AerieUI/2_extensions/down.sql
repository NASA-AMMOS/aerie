comment on column extension_roles.extension_id is null;
comment on column extension_roles.role is null;
comment on table extension_roles is null;

drop table extension_roles;

drop trigger extensions_set_timestamp on extensions;
drop function extensions_set_updated_at();

comment on column extensions.description is null;
comment on column extensions.label is null;
comment on column extensions.owner is null;
comment on column extensions.url is null;
comment on column extensions.updated_at is null;
comment on table extensions is null;

drop table extensions;

call migrations.mark_migration_rolled_back('2');
