create table migrations.schema_migrations (
  migration_id varchar primary key
);

create procedure migrations.mark_migration_applied(_migration_id varchar)
language plpgsql as $$
begin
  insert into migrations.schema_migrations (migration_id)
  values (_migration_id);
end;
$$;

create procedure migrations.mark_migration_rolled_back(_migration_id varchar)
language plpgsql as $$
begin
  delete from migrations.schema_migrations
  where migration_id = _migration_id;
end;
$$;

comment on schema migrations is e''
  'Tables and procedures associated with tracking schema migrations';
comment on table migrations.schema_migrations is e''
  'Tracks what migrations have been applied';
comment on column migrations.schema_migrations.migration_id is e''
  'An identifier for a migration that has been applied';
comment on procedure migrations.mark_migration_applied is e''
  'Given an identifier for a migration, add that migration to the applied set';
comment on procedure migrations.mark_migration_rolled_back is e''
  'Given an identifier for a migration, remove that migration from the applied set';
