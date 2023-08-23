delete from constraint_run;

alter table constraint_run rename column results TO violations;

call migrations.mark_migration_rolled_back('25');
