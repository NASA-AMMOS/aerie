comment on column activity_type.units is null;
alter table activity_type
  drop column units;

call migrations.mark_migration_rolled_back('22');
