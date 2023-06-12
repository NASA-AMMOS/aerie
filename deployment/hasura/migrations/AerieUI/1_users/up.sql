comment on column view.owner is null;
alter table view
  drop column owner;
alter table view
  add column owner integer;
comment on column view.owner is e''
  'The user who owns the view.';

call migrations.mark_migration_applied('1');
