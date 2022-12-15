create table "schema_migrations" (
  "version" varchar not null primary key
);

create function mark_migration_applied(migration_version varchar)
returns void
language plpgsql as $$
begin
  insert into schema_migrations (version)
  values (migration_version);
end;
$$;

create function mark_migration_rolled_back(migration_version varchar)
returns void
language plpgsql as $$
begin
  delete from schema_migrations
  where version = migration_version;
end;
$$;

select mark_migration_applied('0');
