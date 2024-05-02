create table scheduling_model_specification_goals(
  model_id integer not null,
  goal_id integer not null,
  goal_revision integer, -- latest is NULL
  priority integer not null,

  primary key (model_id, goal_id),
  foreign key (goal_id)
    references scheduling_goal_metadata
    on update cascade
    on delete restrict,
  foreign key (goal_id, goal_revision)
    references scheduling_goal_definition
    on update cascade
    on delete restrict,
  constraint model_spec_unique_goal_priorities
    unique (model_id, priority) deferrable initially deferred,
  constraint model_spec_nonnegative_priority
    check (priority >= 0)
);

comment on table scheduling_model_specification_goals is e''
'The set of scheduling goals that all plans using the model should include in their scheduling specification.';
comment on column scheduling_model_specification_goals.model_id is e''
'The model which this specification is for. Half of the primary key.';
comment on column scheduling_model_specification_goals.goal_id is e''
'The id of a specific scheduling goal in the specification. Half of the primary key.';
comment on column scheduling_model_specification_goals.goal_revision is e''
'The version of the scheduling goal definition to use. Leave NULL to use the latest version.';
comment on column scheduling_model_specification_goals.priority is e''
  'The relative priority of the scheduling goal in relation to other goals on the same specification.';

create function insert_scheduling_model_specification_goal_func()
  returns trigger
  language plpgsql as $$
  declare
    next_priority integer;
begin
  select coalesce(
    (select priority
     from scheduling_model_specification_goals smg
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

  update scheduling_model_specification_goals
  set priority = priority + 1
  where model_id = new.model_id
    and priority >= new.priority;
  return new;
end;
$$;

comment on function insert_scheduling_model_specification_goal_func() is e''
  'Checks that the inserted priority is consecutive, and reorders (increments) higher or equal priorities to make room.';

create trigger insert_scheduling_model_specification_goal
  before insert
  on scheduling_model_specification_goals
  for each row
execute function insert_scheduling_model_specification_goal_func();

create function update_scheduling_model_specification_goal_func()
  returns trigger
  language plpgsql as $$
  declare
    next_priority integer;
begin
  select coalesce(
    (select priority
     from scheduling_model_specification_goals smg
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
    update scheduling_model_specification_goals
    set priority = priority - 1
    where model_id = new.model_id
      and priority between old.priority + 1 and new.priority
      and goal_id != new.goal_id;
  else
    update scheduling_model_specification_goals
    set priority = priority + 1
    where model_id = new.model_id
      and priority between new.priority and old.priority - 1
      and goal_id != new.goal_id;
  end if;
  return new;
end;
$$;

comment on function update_scheduling_model_specification_goal_func() is e''
  'Checks that the updated priority is consecutive, and reorders priorities to make room.';

create trigger update_scheduling_model_specification_goal
  before update
  on scheduling_model_specification_goals
  for each row
  when (OLD.priority is distinct from NEW.priority and pg_trigger_depth() < 1)
execute function update_scheduling_model_specification_goal_func();

create function delete_scheduling_model_specification_goal_func()
  returns trigger
  language plpgsql as $$
  declare
    r scheduling_model_specification_goals;
begin
  -- Perform updates in reverse-priority order to ensure that there are no gaps
  for r in select * from removed_rows order by priority desc loop
    update scheduling_model_specification_goals
    set priority = priority - 1
    where model_id = r.model_id
      and priority > r.priority;
  end loop;
  return null;
end;
$$;

comment on function delete_scheduling_model_specification_goal_func() is e''
  'Reorders (decrements) priorities to fill the gap from deleted priority.';

create trigger delete_scheduling_model_specification_goal
  after delete on scheduling_model_specification_goals
  referencing old table as removed_rows
  for each statement
execute function delete_scheduling_model_specification_goal_func();
