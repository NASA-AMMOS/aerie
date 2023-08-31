delete from constraint_run;

alter table constraint_run rename column results TO violations;

comment on column constraint_run.violations is e''
  'Any violations that were found during the constraint check.';

call migrations.mark_migration_rolled_back('25');
