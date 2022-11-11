create table scheduling_specification (
  id integer generated always as identity,
  revision integer not null default 0,

  plan_id integer not null,
  plan_revision integer not null,
  horizon_start timestamptz not null,
  horizon_end timestamptz not null,
  simulation_arguments jsonb not null,
  analysis_only boolean not null,
  constraint scheduling_specification_synthetic_key
    primary key(id),
  constraint scheduling_specification_unique_per_plan
    unique (plan_id)
);

comment on table scheduling_specification is e''
  'The specification for a scheduling run.';
comment on column scheduling_specification.id is e''
  'The synthetic identifier for this scheduling specification.';
comment on column scheduling_specification.revision is e''
  'A monotonic clock that ticks for every change to this scheduling specification.';
comment on column scheduling_specification.plan_id is e''
  'The ID of the plan to be scheduled.';
comment on column scheduling_specification.horizon_start is e''
  'The start of the scheduling horizon within which the scheduler may place activities.';
comment on column scheduling_specification.horizon_end is e''
  'The end of the scheduling horizon within which the scheduler may place activities.';
comment on column scheduling_specification.simulation_arguments is e''
  'The arguments to use for simulation during scheduling.';
comment on column scheduling_specification.analysis_only is e''
  'The boolean stating whether this is an analysis run only';

create function increment_revision_on_update()
  returns trigger
  security definer
language plpgsql as $$begin
  new.revision = old.revision + 1;
return new;
end$$;

create function increment_revision_on_goal_update()
  returns trigger
  security definer
language plpgsql as $$begin
  with goals as (
    select g.specification_id from scheduling_specification_goals as g
    where g.specification_id = new.id
  )
  update scheduling_specification set revision = revision + 1
  where exists(select 1 from goals where specification_id = id);
return new;
end$$;

create trigger increment_revision_on_update_trigger
  before update on scheduling_specification
  for each row
  when (pg_trigger_depth() < 1)
  execute function increment_revision_on_update();

create trigger increment_revision_on_goal_update
  before update on scheduling_goal
  for each row
  execute function increment_revision_on_goal_update();
