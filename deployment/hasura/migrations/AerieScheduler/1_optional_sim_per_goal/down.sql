comment on column scheduling_specification_goals.simulate_after is null;
alter table scheduling_specification_goals drop column simulate_after;

call migrations.mark_migration_rolled_back('1');
