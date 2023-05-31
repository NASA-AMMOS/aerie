alter table scheduling_condition
  alter column description drop default,
  alter column description drop not null;

alter table scheduling_goal
  alter column description drop default,
  alter column description drop not null;

call migrations.mark_migration_rolled_back('7');
