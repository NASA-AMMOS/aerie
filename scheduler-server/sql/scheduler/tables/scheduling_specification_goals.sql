create table scheduling_specification_goals (
  specification_id integer not null,
  goal_id integer not null,
  priority integer
    not null
    default null -- Nulls are detected and replaced with the next
                 -- available priority by the insert trigger
    constraint non_negative_specification_goal_priority check (priority >= 0),
  enabled boolean default true,

  constraint scheduling_specification_goals_primary_key
    primary key (specification_id, goal_id),
  constraint scheduling_specification_goals_unique_priorities
    unique (specification_id, priority) deferrable initially deferred,
  constraint scheduling_specification_goals_references_scheduling_specification
    foreign key (specification_id)
      references scheduling_specification
      on update cascade
      on delete cascade,
  constraint scheduling_specification_goals_references_scheduling_goals
    foreign key (goal_id)
      references scheduling_goal
      on update cascade
      on delete cascade,
  constraint scheduling_specification_unique_goal_id
    unique (goal_id)
);

comment on table scheduling_specification_goals is e''
  'A join table associating scheduling specifications with scheduling goals.';
comment on column scheduling_specification_goals.specification_id is e''
  'The ID of the scheduling specification a scheduling goal is associated with.';
comment on column scheduling_specification_goals.goal_id is e''
  'The ID of the scheduling goal a scheduling specification is associated with.';
comment on column scheduling_specification_goals.priority is e''
  'The relative priority of a scheduling goal in relation to other '
  'scheduling goals within the same specification.';

create or replace function insert_scheduling_specification_goal_func()
  returns trigger as $$begin
    if NEW.priority IS NULL then
      NEW.priority = (
        select coalesce(max(priority), -1) from scheduling_specification_goals
        where specification_id = NEW.specification_id
      ) + 1;
    elseif NEW.priority > (
      select coalesce(max(priority), -1) from scheduling_specification_goals
        where specification_id = new.specification_id
    ) + 1 then
      raise exception 'Inserted priority % for specification_id % is not consecutive', NEW.priority, NEW.specification_id;
    end if;
    update scheduling_specification_goals
      set priority = priority + 1
      where specification_id = NEW.specification_id
        and priority >= NEW.priority;
    return NEW;
  end;$$
language plpgsql;

comment on function insert_scheduling_specification_goal_func is e''
  'Checks that the inserted priority is consecutive, and reorders (increments) higher or equal priorities to make room.';

create or replace function update_scheduling_specification_goal_func()
  returns trigger as $$begin
  if (pg_trigger_depth() = 1) then
      if NEW.priority > OLD.priority then
        if NEW.priority > (
          select coalesce(max(priority), -1) from scheduling_specification_goals
            where specification_id = new.specification_id
        ) + 1 then
          raise exception 'Updated priority % for specification_id % is not consecutive', NEW.priority, new.specification_id;
        end if;
        update scheduling_specification_goals
          set priority = priority - 1
          where specification_id = NEW.specification_id
            and priority between OLD.priority + 1 and NEW.priority
            and goal_id <> NEW.goal_id;
      else
        update scheduling_specification_goals
          set priority = priority + 1
          where specification_id = NEW.specification_id
            and priority between NEW.priority and OLD.priority - 1
            and goal_id <> NEW.goal_id;
      end if;
    end if;
    return NEW;
  end;$$
language plpgsql;

comment on function update_scheduling_specification_goal_func is e''
  'Checks that the updated priority is consecutive, and reorders priorities to make room.';

create function delete_scheduling_specification_goal_func()
  returns trigger as $$begin
    update scheduling_specification_goals
      set priority = priority - 1
      where specification_id = OLD.specification_id
        and priority > OLD.priority;
    return null;
  end;$$
language plpgsql;

comment on function delete_scheduling_specification_goal_func() is e''
  'Reorders (decrements) priorities to fill the gap from deleted priority.';

create trigger insert_scheduling_specification_goal
  before insert
  on scheduling_specification_goals
  for each row
execute function insert_scheduling_specification_goal_func();

create trigger update_scheduling_specification_goal
  before update
  on scheduling_specification_goals
  for each row
  when (OLD.priority is distinct from NEW.priority)
execute function update_scheduling_specification_goal_func();

create trigger delete_scheduling_specification_goal
  after delete
  on scheduling_specification_goals
  for each row
execute function delete_scheduling_specification_goal_func();
