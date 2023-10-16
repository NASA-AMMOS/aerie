begin;
  -- Schema migrations
  \ir tables/schema_migrations.sql
  \ir applied_migrations.sql

  -- Tables.
  \ir tables/extensions.sql
  \ir tables/extension_roles.sql
  \ir tables/view.sql
end;
