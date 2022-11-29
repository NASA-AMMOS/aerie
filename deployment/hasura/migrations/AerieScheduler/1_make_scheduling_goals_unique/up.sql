/*
make_scheduling_goals_unique

The purpose of this migration is to limit the flexibility of scheduling specifications. The current schema allows for
the same goal to be part of multiple scheduling specifications, which can be problematic when that goal is mutated.

This mutation imposes a constraint that every goal must be part of no more than one specification.

It is desirable to keep API compatibility with the previous schema.

This sql file will fail if there are pre-existing goals that are members of multiple specifications. These cases must
either be addressed manually, or with the help of prepare.sql.
*/

alter table scheduling_specification_goals add constraint scheduling_specification_unique_goal_id unique (goal_id);

select mark_migration_applied('1');
