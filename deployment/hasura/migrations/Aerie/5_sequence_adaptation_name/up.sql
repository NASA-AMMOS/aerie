alter table sequencing.sequence_adaptation
  add column name text not null,
  add constraint sequence_adaptation_natural_key
    unique (name);

comment on column sequencing.sequence_adaptation.name is e''
  'The name of the sequence adaptation.';

call migrations.mark_migration_applied('5');
