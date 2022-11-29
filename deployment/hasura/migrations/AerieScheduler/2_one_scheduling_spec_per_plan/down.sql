alter table scheduling_specification drop constraint if exists scheduling_specification_unique_plan_id;

select mark_migration_rolled_back('2');
