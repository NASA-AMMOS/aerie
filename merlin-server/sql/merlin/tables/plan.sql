create table plan (
  id integer generated always as identity check ( id > 0 ),
  revision integer not null default 0,

  name text not null,
  model_id integer null,
  duration interval not null,

  -- TODO: Remove 'start_time'; its purpose will be served by a model-specific entry in 'simulation.arguments'.
  start_time timestamptz not null,
  parent_id integer
    references plan
    on update cascade,

  constraint plan_synthetic_key
    primary key (id),
  constraint plan_natural_key
    unique (name),
  constraint plan_uses_model
    foreign key (model_id)
    references mission_model
    on update cascade
    on delete set null
);

create index plan_model_id_index on plan (model_id);


comment on table plan is e''
  'A set of activities scheduled against a mission model.';

comment on column plan.id is e''
  'The synthetic identifier for this plan.';
comment on column plan.revision is e''
  'A monotonic clock that ticks for every change to this plan.';
comment on column plan.name is e''
  'A human-readable name for this plan. Unique amongst all plans.';
comment on column plan.model_id is e''
  'The mission model used to simulate and validate the plan.'
'\n'
  'May be NULL if the mission model the plan references has been deleted.';
comment on column plan.duration is e''
  'The duration over which this plan extends.';
comment on column plan.start_time is e''
  'DEPRECATED. The time at which the plan''s effective span begins.';


create function increment_revision_on_update_plan()
returns trigger
security definer
language plpgsql as $$begin
  update plan
  set revision = revision + 1
  where id = new.id
    or id = old.id;

  return new;
end$$;

create trigger increment_revision_on_update_plan_trigger
after update on plan
for each row
when (pg_trigger_depth() < 1)
execute function increment_revision_on_update_plan();

create function raise_duration_is_negative()
returns trigger
security definer
language plpgsql as $$begin
  raise exception 'invalid plan duration, expected nonnegative duration but found: %', new.duration;
end$$;

create trigger check_plan_duration_is_nonnegative_trigger
before insert or update on plan
for each row
when (new.duration < '0')
execute function raise_duration_is_negative();

create or replace function cleanup_on_delete()
  returns trigger
  language plpgsql as $$
begin
  -- have the children be 'adopted' on delete
  update plan
  set parent_id = old.parent_id
  where
    parent_id = old.id;
  return old;
end
$$;

create trigger cleanup_on_delete_trigger
  before delete on plan
  for each row
execute function cleanup_on_delete();
