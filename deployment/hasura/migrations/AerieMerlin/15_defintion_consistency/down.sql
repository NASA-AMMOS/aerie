alter table public.simulation_template
  alter column description drop default;

alter table public."constraint"
  alter column description drop default;

call migrations.mark_migration_rolled_back('15');
