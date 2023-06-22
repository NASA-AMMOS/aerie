alter table scheduling_goal
add column tags text[] not null default '{}';
comment on column scheduling_goal.tags is e''
  'The tags associated with this scheduling goal.';

comment on table metadata.scheduling_goal_tags is null;
drop table metadata.scheduling_goal_tags;
drop schema metadata;

call migrations.mark_migration_rolled_back('8');
