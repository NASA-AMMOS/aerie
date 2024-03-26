begin;
---------------------
-- UPDATE TRIGGERS --
---------------------
create or replace trigger increment_revision_on_update_trigger
  before update on scheduler.scheduling_specification
  for each row
  when (pg_trigger_depth() < 1)
  execute function util_functions.increment_revision_update();
drop function scheduler.increment_revision_on_update();

create or replace trigger set_timestamp
before update on scheduler.scheduling_condition_metadata
for each row
execute function util_functions.set_updated_at();
drop function scheduler.scheduling_condition_metadata_set_updated_at();

create or replace trigger set_timestamp
before update on scheduler.scheduling_goal_metadata
for each row
execute function util_functions.set_updated_at();
drop function scheduler.scheduling_goal_metadata_set_updated_at();

create function scheduler.create_scheduling_spec_for_new_plan()
returns trigger
security definer
language plpgsql as $$
declare
  spec_id integer;
begin
  -- Create a new scheduling specification
  insert into scheduler.scheduling_specification (revision, plan_id, plan_revision, horizon_start, horizon_end,
                                                  simulation_arguments, analysis_only)
  values (0, new.id, new.revision, new.start_time, new.start_time+new.duration, '{}', false)
  returning id into spec_id;

  -- Populate the scheduling specification
  insert into scheduler.scheduling_specification_goals (specification_id, goal_id, goal_revision, priority)
  select spec_id, msg.goal_id, msg.goal_revision, msg.priority
  from scheduler.scheduling_model_specification_goals msg
  where msg.model_id = new.model_id;

  insert into scheduler.scheduling_specification_conditions (specification_id, condition_id, condition_revision)
  select spec_id, msc.condition_id, msc.condition_revision
  from scheduler.scheduling_model_specification_conditions msc
  where msc.model_id = new.model_id;

  return new;
end
$$;

comment on function scheduler.create_scheduling_spec_for_new_plan() is e''
'Creates a scheduling specification for a new plan
 and populates it with the contents of the plan''s model''s specification.';

create trigger scheduling_spec_for_new_plan_trigger
after insert on merlin.plan
for each row
execute function scheduler.create_scheduling_spec_for_new_plan();

---------------------------------
-- UPDATE FUNCTION DEFINITIONS --
---------------------------------
create or replace function scheduler.cancel_pending_scheduling_rqs()
returns trigger
security definer
language plpgsql as $$
begin
  update scheduler.scheduling_request
  set canceled = true
  where status = 'pending'
  and specification_id = new.specification_id;
  return new;
end
$$;

create or replace function scheduler.insert_scheduling_model_specification_goal_func()
  returns trigger
  language plpgsql as $$
  declare
    next_priority integer;
begin
  select coalesce(
    (select priority
     from scheduler.scheduling_model_specification_goals smg
     where smg.model_id = new.model_id
     order by priority desc
     limit 1), -1) + 1
  into next_priority;

  if new.priority > next_priority then
    raise numeric_value_out_of_range using
      message = ('Updated priority % for model_id % is not consecutive', new.priority, new.model_id),
      hint = ('The next available priority is %.', next_priority);
  end if;

  if new.priority is null then
    new.priority = next_priority;
  end if;

  update scheduler.scheduling_model_specification_goals
  set priority = priority + 1
  where model_id = new.model_id
    and priority >= new.priority;
  return new;
end;
$$;

create or replace function scheduler.update_scheduling_model_specification_goal_func()
  returns trigger
  language plpgsql as $$
  declare
    next_priority integer;
begin
  select coalesce(
    (select priority
     from scheduler.scheduling_model_specification_goals smg
     where smg.model_id = new.model_id
     order by priority desc
     limit 1), -1) + 1
  into next_priority;

  if new.priority > next_priority then
    raise numeric_value_out_of_range using
      message = ('Updated priority % for model_id % is not consecutive', new.priority, new.model_id),
      hint = ('The next available priority is %.', next_priority);
  end if;

  if new.priority > old.priority then
    update scheduler.scheduling_model_specification_goals
    set priority = priority - 1
    where model_id = new.model_id
      and priority between old.priority + 1 and new.priority
      and goal_id != new.goal_id;
  else
    update scheduler.scheduling_model_specification_goals
    set priority = priority + 1
    where model_id = new.model_id
      and priority between new.priority and old.priority - 1
      and goal_id != new.goal_id;
  end if;
  return new;
end;
$$;

create or replace function scheduler.delete_scheduling_model_specification_goal_func()
  returns trigger
  language plpgsql as $$
begin
  update scheduler.scheduling_model_specification_goals
  set priority = priority - 1
  where model_id = old.model_id
    and priority > old.priority;
  return null;
end;
$$;

create or replace function scheduler.increment_spec_revision_on_conditions_spec_update()
  returns trigger
  security definer
language plpgsql as $$
begin
  update scheduler.scheduling_specification
  set revision = revision + 1
  where id = new.specification_id;
  return new;
end;
$$;

create or replace function scheduler.increment_spec_revision_on_conditions_spec_delete()
  returns trigger
  security definer
language plpgsql as $$
begin
  update scheduler.scheduling_specification
  set revision = revision + 1
  where id = new.specification_id;
  return new;
end;
$$;

create or replace function scheduler.insert_scheduling_specification_goal_func()
  returns trigger
  language plpgsql as $$
  declare
    next_priority integer;
begin
  select coalesce(
    (select priority
     from scheduler.scheduling_specification_goals ssg
     where ssg.specification_id = new.specification_id
     order by priority desc
     limit 1), -1) + 1
  into next_priority;

  if new.priority > next_priority then
    raise numeric_value_out_of_range using
      message = ('Updated priority % for specification_id % is not consecutive', new.priority, new.specification_id),
      hint = ('The next available priority is %.', next_priority);
  end if;

  if new.priority is null then
    new.priority = next_priority;
  end if;

  update scheduler.scheduling_specification_goals
  set priority = priority + 1
  where specification_id = new.specification_id
    and priority >= new.priority;
  return new;
end;
$$;

create or replace function scheduler.update_scheduling_specification_goal_func()
  returns trigger
  language plpgsql as $$
  declare
    next_priority integer;
begin
  select coalesce(
    (select priority
     from scheduler.scheduling_specification_goals ssg
     where ssg.specification_id = new.specification_id
     order by priority desc
     limit 1), -1) + 1
  into next_priority;

  if new.priority > next_priority then
    raise numeric_value_out_of_range using
      message = ('Updated priority % for specification_id % is not consecutive', new.priority, new.specification_id),
      hint = ('The next available priority is %.', next_priority);
  end if;

  if new.priority > old.priority then
    update scheduler.scheduling_specification_goals
    set priority = priority - 1
    where specification_id = new.specification_id
      and priority between old.priority + 1 and new.priority
      and goal_id != new.goal_id;
  else
    update scheduler.scheduling_specification_goals
    set priority = priority + 1
    where specification_id = new.specification_id
      and priority between new.priority and old.priority - 1
      and goal_id != new.goal_id;
  end if;
  return new;
end;
$$;

create or replace function scheduler.delete_scheduling_specification_goal_func()
  returns trigger
  language plpgsql as $$
begin
  update scheduler.scheduling_specification_goals
  set priority = priority - 1
  where specification_id = old.specification_id
    and priority > old.priority;
  return null;
end;
$$;

create or replace function scheduler.increment_spec_revision_on_goal_spec_update()
  returns trigger
  security definer
language plpgsql as $$begin
  update scheduler.scheduling_specification
  set revision = revision + 1
  where id = new.specification_id;
  return new;
end$$;

create or replace function scheduler.increment_spec_revision_on_goal_spec_delete()
  returns trigger
  security definer
language plpgsql as $$begin
  update scheduler.scheduling_specification
  set revision = revision + 1
  where id = old.specification_id;
  return old;
end$$;

create or replace function scheduler.scheduling_condition_definition_set_revision()
returns trigger
volatile
language plpgsql as $$
declare
  max_revision integer;
begin
  -- Grab the current max value of revision, or -1, if this is the first revision
  select coalesce((select revision
  from scheduler.scheduling_condition_definition
  where condition_id = new.condition_id
  order by revision desc
  limit 1), -1)
  into max_revision;

  new.revision = max_revision + 1;
  return new;
end
$$;

create or replace function scheduler.scheduling_goal_definition_set_revision()
returns trigger
volatile
language plpgsql as $$
declare
  max_revision integer;
begin
  -- Grab the current max value of revision, or -1, if this is the first revision
  select coalesce((select revision
  from scheduler.scheduling_goal_definition
  where goal_id = new.goal_id
  order by revision desc
  limit 1), -1)
  into max_revision;

  new.revision = max_revision + 1;
  return new;
end
$$;

end;
