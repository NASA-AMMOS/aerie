-- Drop notify
drop trigger notify_simulation_workers_cancel on simulation_dataset;
drop function notify_simulation_workers_cancel();

-- "Recancel" all old sims
with latest_revisions(simulation_id, simulation_rev, plan_rev, model_rev, sim_template_rev) as (
  select s.id, s.revision, p.revision, m.revision, st.revision
  from simulation s
  left join plan p on s.plan_id = p.id
  left join mission_model m on p.model_id = m.id
  left join simulation_template st on s.simulation_template_id = st.id
)
update simulation_dataset sd
set canceled = true
from latest_revisions lr
where sd.simulation_id = lr.simulation_id
  and (sd.plan_revision is distinct from lr.plan_rev
    or sd.model_revision is distinct from lr.model_rev
    or sd.simulation_template_revision is distinct from lr.sim_template_rev
    or sd.simulation_revision is distinct from lr.simulation_rev);

-- Restore cancel triggers
create function cancel_on_mission_model_update()
returns trigger
security definer
language plpgsql as $$begin
  with
    sim_info as
      ( select
          sim.id as sim_id,
          model.id as model_id
        from simulation as sim
        left join plan on sim.plan_id = plan.id
        left join mission_model as model on plan.model_id = model.id)
  update simulation_dataset
  set canceled = true
  where simulation_id in (select sim_id from sim_info where model_id = new.id);
return new;
end$$;

create function cancel_on_plan_update()
returns trigger
security definer
language plpgsql as $$begin
  update simulation_dataset
  set canceled = true
  where simulation_id in (select id from simulation where plan_id = new.id);
return new;
end$$;

create function cancel_on_simulation_update()
returns trigger
security definer
language plpgsql as $$begin
  update simulation_dataset
  set canceled = true
  where simulation_id = new.id;
return new;
end$$;

create function cancel_on_simulation_template_update()
returns trigger
security definer
language plpgsql as $$begin
  update simulation_dataset
  set canceled = true
  where simulation_id in (select id from simulation where simulation_template_id = new.id);
return new;
end$$;

do $$ begin
create trigger cancel_on_mission_model_update_trigger
  after update on mission_model
  for each row
  execute function cancel_on_mission_model_update();
exception
  when duplicate_object then null;
end $$;

do $$ begin
create trigger cancel_on_plan_update_trigger
  after update on plan
  for each row
  execute function cancel_on_plan_update();
exception
  when duplicate_object then null;
end $$;

do $$ begin
create trigger cancel_on_simulation_update_trigger
  after update on simulation
  for each row
  execute function cancel_on_simulation_update();
exception
  when duplicate_object then null;
end $$;

do $$ begin
create trigger cancel_on_simulation_template_update_trigger
  after update on simulation_template
  for each row
  execute function cancel_on_simulation_template_update();
exception
  when duplicate_object then null;
end $$;

call migrations.mark_migration_rolled_back('33');
