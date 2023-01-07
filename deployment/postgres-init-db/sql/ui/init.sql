begin;
  -- Schema migrations
  \ir tables/schema_migrations.sql
  \ir applied_migrations.sql

  -- Tables.
  \ir tables/view.sql
end;
