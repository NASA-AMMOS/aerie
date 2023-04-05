alter table public.simulation_template
  add column simulation_start_time timestamptz default null,
  add column simulation_end_time timestamptz default null;

alter table public.simulation
  alter column simulation_start_time drop not null,
  alter column simulation_end_time drop not null,
  drop constraint simulation_end_after_simulation_start;

create or replace function create_simulation_row_for_new_plan()
returns trigger
security definer
language plpgsql as $$begin
  insert into simulation (revision, simulation_template_id, plan_id, arguments)
  values (0, null, new.id, '{}');
  return new;
end
$$;

call migrations.mark_migration_rolled_back('11');
