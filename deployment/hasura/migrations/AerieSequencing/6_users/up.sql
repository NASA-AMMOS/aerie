-- User Sequence
comment on column user_sequence.owner is null;

alter table user_sequence
  alter column owner drop not null,
  alter column owner drop default;
update user_sequence
  set owner = null
  where owner = 'unknown';

comment on column user_sequence.owner is e''
  'The user responsible for this sequence.';

-- Expansion Set
alter table expansion_set
  alter column owner drop not null,
  alter column owner drop default,
  alter column updated_by drop not null,
  alter column updated_by drop default;
update expansion_set
  set owner = null
  where owner = '';
update expansion_set
  set updated_by = null
  where updated_by = '';

-- Expansion Rule
alter table expansion_rule
  alter column owner drop not null,
  alter column owner drop default,
  alter column updated_by drop not null,
  alter column updated_by drop default;
update expansion_rule
  set owner = null
  where owner = '';
update expansion_rule
  set updated_by = null
  where updated_by = '';

call migrations.mark_migration_applied('6');
