alter table simulation drop column offset_from_plan_start;
alter table simulation drop column duration;
alter table simulation_template drop column offset_from_plan_start;
alter table simulation_template drop column duration;

call migrations.mark_migration_rolled_back('4');
