-- Add new columns
alter table simulation_dataset
add column arguments merlin_argument_set,
add column simulation_start_time timestamptz,
add column simulation_end_time timestamptz;

-- Fill in new columns
with reference as (
  select s.id as sim_id, p.id as plan_id, p.start_time as start_time, p.duration as duration
  from simulation s, plan p
  where s.plan_id = p.id
)
update simulation_dataset sd
set arguments = '{}',
    simulation_start_time = r.start_time + offset_from_plan_start,
    simulation_end_time = r.start_time + r.duration
from reference r
where sd.simulation_id = r.sim_id;

-- Add not null constraint to new columns
alter table simulation_dataset
alter column arguments set not null,
alter column simulation_start_time set not null,
alter column simulation_end_time set not null,
add constraint start_before_end check (simulation_start_time <= simulation_end_time);

-- Add Trigger to update offset_from_plan
create function update_offset_from_plan_start()
returns trigger
security invoker
language plpgsql as $$
declare
  plan_start timestamptz;
begin
  select p.start_time
  from simulation s, plan p
  where s.plan_id = p.id
    and new.simulation_id = s.id
  into plan_start;

  new.offset_from_plan_start = new.simulation_start_time - plan_start;
  return new;
end
$$;

create trigger update_offset_from_plan_start_trigger
before insert or update on simulation_dataset
for each row
execute function update_offset_from_plan_start();

-- Alter the view
drop view simulated_activity;
create view simulated_activity as
(
  select span.id as id,
         simulation_dataset.id as simulation_dataset_id,
         span.parent_id as parent_id,
         span.start_offset as start_offset,
         span.duration as duration,
         span.attributes as attributes,
         span.type as activity_type_name,
         (span.attributes#>>'{directiveId}')::integer as directive_id,
         simulation_dataset.simulation_start_time + span.start_offset as start_time,
         simulation_dataset.simulation_start_time + span.start_offset + span.duration as end_time
   from span
     join dataset on span.dataset_id = dataset.id
     join simulation_dataset on dataset.id = simulation_dataset.dataset_id
     join simulation on simulation.id = simulation_dataset.simulation_id
);

comment on view simulated_activity is e''
  'Concrete activity instance created via simulation.';
comment on column simulated_activity.id is e''
  'Unique identifier for the activity instance span.';
comment on column simulated_activity.simulation_dataset_id is e''
  'The simulation dataset this activity is part of.';
comment on column simulated_activity.parent_id is e''
  'The parent activity of this activity.';
comment on column simulated_activity.start_offset is e''
  'The offset from the dataset start at which this activity begins.';
comment on column simulated_activity.duration is e''
  'The amount of time this activity extends for.';
comment on column simulated_activity.attributes is e''
  'A set of named values annotating this activity.';
comment on column simulated_activity.activity_type_name is e''
  'The activity type of this activity.';
comment on column simulated_activity.directive_id is e''
  'The id of the activity directive that created this activity.';
comment on column simulated_activity.start_time is e''
  'The absolute start time of this activity.';
comment on column simulated_activity.end_time is e''
  'The absolute end time of this activity.';
