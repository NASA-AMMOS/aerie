comment on column view.owner is null;
alter table view
  alter column owner set default 'system';
update view
  set owner = default
  where owner is null;
alter table view
  alter column owner set not null;
comment on column view.owner is e''
  'Username of the view owner.';

call migrations.mark_migration_rolled_back('1');
