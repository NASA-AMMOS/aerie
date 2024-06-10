alter table sequencing.sequence_adaptation
  drop column name,
  drop constraint sequence_adaptation_natural_key;

call migrations.mark_migration_rolled_back('5');
