begin;
  -- Tables.
  \ir tables/view.sql

  -- Seed UI View Data.
  \set view_0 `cat /docker-entrypoint-initdb.d/data/ui/views/example_0.json`
  \set view_1 `cat /docker-entrypoint-initdb.d/data/ui/views/example_1.json`
  insert into view (definition, name) values (:'view_0', 'Banananation');
  insert into view (definition, name) values (:'view_1', 'InSight');
end;
