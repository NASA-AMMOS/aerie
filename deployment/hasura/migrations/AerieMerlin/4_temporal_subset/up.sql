alter table simulation add column offset_from_plan_start interval default null;
alter table simulation add column duration interval default null;
alter table simulation_template add column offset_from_plan_start interval default null;
alter table simulation_template add column duration interval default null;

call migrations.mark_migration_applied('4');
