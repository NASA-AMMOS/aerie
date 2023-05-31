alter table scheduling_goal
  alter column description set default '',
  alter column description set not null;

alter table scheduling_condition
  alter column description set default '',
  alter column description set not null;

call migrations.mark_migration_applied('7');
