-- Tags
comment on column metadata.tags.created_at is null;
comment on column metadata.tags.owner is null;
comment on column metadata.tags.color is null;
comment on column metadata.tags.name is null;
comment on column metadata.tags.id is null;
comment on table metadata.tags is null;

drop table metadata.tags;
drop schema metadata;

call migrations.mark_migration_rolled_back('16');
