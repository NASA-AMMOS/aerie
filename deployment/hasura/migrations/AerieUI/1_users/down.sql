comment on column view.owner is null;
alter table view
  drop column owner;
alter table view
  add column owner text not null default 'system';
comment on column view.owner is e''
  'Username of the view owner.';

call migrations.mark_migration_rolled_back('1');
