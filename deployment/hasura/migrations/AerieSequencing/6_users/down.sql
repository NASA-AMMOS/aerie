-- Expansion Rule
alter table expansion_rule
  alter column updated_by set default '',
  alter column owner set default '';
update expansion_rule
  set updated_by = default
  where updated_by is null;
update expansion_rule
  set owner = default
  where owner is null;
alter table expansion_rule
  alter column updated_by set not null,
  alter column owner set not null;

-- Expansion Set
alter table expansion_set
  alter column updated_by set default '',
  alter column owner set default '';
update expansion_set
  set updated_by = default
  where updated_by is null;
update expansion_set
  set owner = default
  where owner is null;
alter table expansion_set
  alter column updated_by set not null,
  alter column owner set not null;

-- User Sequence
comment on column user_sequence.owner is null;

alter table user_sequence
  alter column owner set default 'unknown';
update user_sequence
  set owner = default
  where owner is null;
alter table user_sequence
  alter column owner set not null;

comment on column user_sequence.owner is e''
  'Username of the user sequence owner.';

call migrations.mark_migration_rolled_back('6');
