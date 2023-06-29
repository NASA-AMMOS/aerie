create type status_t as enum('pending', 'incomplete', 'failed', 'success');

create table simulation_dataset (
  id integer generated always as identity,
  simulation_id integer not null,
  dataset_id integer null,

  -- This column may be removed in the future in favor of simulation_start_time
  offset_from_plan_start interval not null,

  -- Determinant entities
  plan_revision integer not null,
  model_revision integer not null,
  simulation_template_revision integer null,
  simulation_revision integer not null,

  -- Dependent entities
  dataset_revision integer null,

  -- Simulation Arguments
  arguments jsonb not null,
  simulation_start_time timestamptz not null,
  simulation_end_time timestamptz not null,

  -- Simulation state
  status status_t not null default 'pending',
  reason jsonb null,
  canceled boolean not null default false,

  -- Additional Metadata
  requested_by text,
  requested_at timestamptz not null default now(),

  constraint simulation_dataset_synthetic_key
    primary key (id),
  constraint simulation_dataset_dataset_has_a_simulation
    unique (dataset_id),
  constraint simulation_dataset_references_simulation
    foreign key (simulation_id)
    references simulation
    on update cascade
    on delete cascade,
  constraint simulation_dataset_references_dataset
    foreign key (dataset_id)
    references dataset
    on update cascade
    on delete cascade,
  constraint simulation_dataset_requested_by_exists
    foreign key (requested_by) references metadata.users
    on update cascade
    on delete set null,
  constraint start_before_end
    check (simulation_start_time <= simulation_end_time)
);

create index simulation_dataset_simulation_has_many_datasets
  on simulation_dataset (simulation_id);

comment on table simulation_dataset is e''
  'A description of the upstream simulation inputs that determined a given dataset.'
'\n'
  'A new row should be created by providing a simulation_id and offset_from_plan_start only '
  'as the remaining data will be filled in during the insertion';

comment on column simulation_dataset.simulation_id is e''
  'The simulation determining the contents of the associated dataset.';
comment on column simulation_dataset.dataset_id is e''
  'The dataset containing simulated results for the simulation. NULL if the dataset has not been constructed yet.';
comment on column simulation_dataset.plan_revision is e''
  'The revision of the plan corresponding to the given revision of the dataset.';
comment on column simulation_dataset.model_revision is e''
  'The revision of the mission model corresponding to the given revision of the dataset.';
comment on column simulation_dataset.simulation_revision is e''
  'The revision of the simulation corresponding to the given revision of the dataset.';
comment on column simulation_dataset.dataset_revision is e''
  'The revision of the dataset corresponding to the given revisions of the input entities.';
comment on column simulation_dataset.status is e''
  'The status of the simulation for which the dataset is associated.';
comment on column simulation_dataset.reason is e''
  'The reason for failure in the event that simulation fails.';
comment on column simulation_dataset.canceled is e''
  'Whether the simulation has been marked as canceled.';
comment on column simulation_dataset.offset_from_plan_start is e''
  'The time to judge dataset items against relative to the plan start.'
'\n'
  'If the dataset as a whole begins one day before the planning period begins, '
  'then this column should contain the interval ''1 day ago''.';
comment on column simulation_dataset.requested_by is e''
  'The user who requested the simulation.';
comment on column simulation_dataset.requested_at is e''
  'When this simulation dataset was created.';

-- Dataset management triggers
-- These triggers create and delete datasets along with the insert/delete of a simulation_dataset

create or replace function set_revisions_and_initialize_dataset_on_insert()
returns trigger
security definer
language plpgsql as $$
declare
  simulation_ref simulation;
  plan_ref plan;
  model_ref mission_model;
  template_ref simulation_template;
  dataset_ref dataset;
begin
  -- Set the revisions
  select into simulation_ref * from simulation where id = new.simulation_id;
  select into plan_ref * from plan where id = simulation_ref.plan_id;
  select into template_ref * from simulation_template where id = simulation_ref.simulation_template_id;
  select into model_ref * from mission_model where id = plan_ref.model_id;
  new.model_revision = model_ref.revision;
  new.plan_revision = plan_ref.revision;
  new.simulation_template_revision = template_ref.revision;
  new.simulation_revision = simulation_ref.revision;

  -- Create the dataset
  insert into dataset
  default values
  returning * into dataset_ref;
  new.dataset_id = dataset_ref.id;
  new.dataset_revision = dataset_ref.revision;
return new;
end$$;

create or replace function delete_dataset_on_delete()
returns trigger
security definer
language plpgsql as $$begin
  delete from dataset
  where id = old.dataset_id;
return old;
end$$;

do $$ begin
create trigger set_revisions_and_initialize_dataset_on_insert_trigger
  before insert on simulation_dataset
  for each row
  execute function set_revisions_and_initialize_dataset_on_insert();
exception
  when duplicate_object then null;
end $$;

do $$ begin
create trigger delete_dataset_on_delete_trigger
  after delete on simulation_dataset
  for each row
  execute function delete_dataset_on_delete();
exception
  when duplicate_object then null;
end $$;

-- Revision-based triggers
-- These triggers cancel datasets when they became outdated

create or replace function cancel_on_mission_model_update()
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

create or replace function cancel_on_plan_update()
returns trigger
security definer
language plpgsql as $$begin
  update simulation_dataset
  set canceled = true
  where simulation_id in (select id from simulation where plan_id = new.id);
return new;
end$$;

create or replace function cancel_on_simulation_update()
returns trigger
security definer
language plpgsql as $$begin
  update simulation_dataset
  set canceled = true
  where simulation_id = new.id;
return new;
end$$;

create or replace function cancel_on_simulation_template_update()
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

-- Simulation dataset NOTIFY triggers
-- These triggers NOTIFY LISTEN(ing) merlin worker clients of pending simulation requests

create or replace function notify_simulation_workers ()
returns trigger
security definer
language plpgsql as $$
declare
  simulation_ref simulation;
begin
  select into simulation_ref * from simulation where id = new.simulation_id;

  perform (
    with payload(model_revision,
                 plan_revision,
                 simulation_revision,
                 simulation_template_revision,
                 dataset_id,
                 simulation_id,
                 plan_id) as
    (
      select NEW.model_revision,
             NEW.plan_revision,
             NEW.simulation_revision,
             NEW.simulation_template_revision,
             NEW.dataset_id,
             NEW.simulation_id,
             simulation_ref.plan_id
    )
    select pg_notify('simulation_notification', json_strip_nulls(row_to_json(payload))::text)
    from payload
  );
  return null;
end$$;

do $$ begin
create trigger notify_simulation_workers
  after insert on simulation_dataset
  for each row
  execute function notify_simulation_workers();
end $$;

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

create or replace function simulation_dataset_check_constraint_run()
  returns trigger
  security definer
  language plpgsql as $$begin
  if new.simulation_id = old.simulation_id
  then
    update constraint_run
    set status = 'simulation-outdated'
    where old.dataset_id = dataset_id;
  end if;
  return new;
end$$;

create trigger simulation_dataset_check_constraint_run_trigger
  before insert on simulation_dataset
  for each row
execute function simulation_dataset_check_constraint_run();
