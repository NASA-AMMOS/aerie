comment on column view.owner is null;
alter table view
  alter column owner drop not null,
  alter column owner drop default;
update view
  set owner = null
  where owner = 'system';
comment on column view.owner is e''
  'The user who owns the view.';

call migrations.mark_migration_applied('1');
