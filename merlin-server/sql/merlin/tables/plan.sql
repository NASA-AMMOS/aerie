create table plan (
  id integer generated always as identity check ( id > 0 ),
  revision integer not null default 0,

  name text not null,
  model_id integer null,
  duration interval not null,

  start_time timestamptz not null,
  parent_id integer
    references plan
    on update cascade,

  is_locked boolean not null default false,

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
  'The time at which the plan''s effective span begins.';
comment on column plan.parent_id is e''
  'The plan id of the parent of this plan. May be NULL if this plan does not have a parent.';
comment on column plan.is_locked is e''
  'A boolean representing whether this plan can be deleted and if changes can happen to the activities of this plan.';


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

create function cleanup_on_delete()
  returns trigger
  language plpgsql as $$
begin
  -- prevent deletion if the plan is locked
  if old.is_locked then
    raise exception 'Cannot delete locked plan.';
  end if;

  -- withdraw pending rqs
  update merge_request
  set status='withdrawn'
  where plan_id_receiving_changes = old.id
    and status = 'pending';

  -- have the children be 'adopted' by this plan's parent
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

create function create_simulation_row_for_new_plan()
returns trigger
security definer
language plpgsql as $$begin
  insert into simulation (revision, simulation_template_id, plan_id, arguments)
  values (0, null, new.id, '{}');
  return new;
end
$$;

create trigger simulation_row_for_new_plan_trigger
after insert on plan
for each row
execute function create_simulation_row_for_new_plan();
