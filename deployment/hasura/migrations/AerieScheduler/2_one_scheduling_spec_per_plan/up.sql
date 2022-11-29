/*
one_scheduling_spec_per_plan

This migration adds a requirement that no two specifications refer to the same plan.

This script will fail if there are pre-existing specifications that refer to the same plan. This will need to be
resolved manually.
*/

alter table scheduling_specification add constraint scheduling_specification_unique_plan_id unique (plan_id);

select mark_migration_applied('2');
