comment on column constraint_run.constraint_id is null;
comment on column constraint_run.constraint_definition is null;
comment on column constraint_run.simulation_id is null;
comment on column constraint_run.status is null;
comment on column constraint_run.violations is null;
comment on column constraint_run.requested_by is null;
comment on column constraint_run.requested_at is null;
comment on table constraint_run is null;

drop table constraint_run;

drop trigger constraint_check_run_trigger on "constraint";
drop function constraint_check_constraint_run();

drop trigger simulation_dataset_check_constraint_run_trigger on simulation_dataset;
drop function simulation_dataset_check_constraint_run();

call migrations.mark_migration_rolled_back('18');
