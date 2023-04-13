create or replace function create_simulation_row_for_new_plan()
returns trigger
security definer
language plpgsql as $$begin
  insert into simulation (revision, simulation_template_id, plan_id, arguments, simulation_start_time, simulation_end_time)
  values (0, null, new.id, '{}', new.start_time, new.start_time+new.duration);
  return new;
end
$$;

alter table public.simulation_template
  drop column simulation_start_time,
  drop column simulation_end_time;

update simulation s
set simulation_start_time = p.start_time
from plan p
where s.plan_id = p.id
  and s.simulation_start_time is null;

update simulation s
set simulation_end_time = p.start_time+p.duration
from plan p
where s.plan_id = p.id
  and s.simulation_end_time is null;

-- Add not null constraints
alter table public.simulation
  alter column simulation_start_time set not null,
  alter column simulation_end_time set not null,
  add constraint simulation_end_after_simulation_start
    check (simulation_start_time <= simulation_end_time);

call migrations.mark_migration_applied('11');
