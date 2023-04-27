comment on column resource_type.schema is null;
comment on column resource_type.model_id is null;
comment on column resource_type.name is null;
comment on table resource_type is null;

drop table resource_type;

call migrations.mark_migration_rolled_back('13');
