alter table public."constraint"
  alter column description set default '';

alter table public.simulation_template
  alter column description set default '';

call migrations.mark_migration_applied('15');
