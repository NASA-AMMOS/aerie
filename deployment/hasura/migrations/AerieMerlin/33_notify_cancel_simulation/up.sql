-- Turn off the cancel triggers
drop trigger cancel_on_simulation_template_update_trigger on simulation_template;
drop trigger cancel_on_simulation_update_trigger on simulation;
drop trigger cancel_on_plan_update_trigger on plan;
drop trigger cancel_on_mission_model_update_trigger on mission_model;

drop function cancel_on_simulation_template_update();
drop function cancel_on_simulation_update();
drop function cancel_on_plan_update();
drop function cancel_on_mission_model_update();

-- "Uncancel" all completed sims marked as canceled by the auto triggers
update simulation_dataset
set canceled = false
where status = 'success' or status = 'failed';

-- Send notify on cancel
create function notify_simulation_workers_cancel()
returns trigger
security definer
language plpgsql as $$
begin
  perform pg_notify('simulation_cancel', '' || new.dataset_id);
  return null;
end
$$;

create trigger notify_simulation_workers_cancel
after update of canceled on simulation_dataset
for each row
when ( (old.status != 'success' or old.status != 'failed') and new.canceled )
execute function notify_simulation_workers_cancel();

call migrations.mark_migration_applied('33');

