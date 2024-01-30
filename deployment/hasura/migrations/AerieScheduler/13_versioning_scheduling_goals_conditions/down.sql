/**********
SCHEDULING GOALS
***********/
/*
RESTORE ORIGINAL
*/
create table scheduling_goal (
  id integer generated always as identity,
  revision integer not null default 0,
  name text not null,
  definition text not null,

  model_id integer not null,
  description text not null default '',
  author text null,
  last_modified_by text null,
  created_date timestamptz not null default now(),
  modified_date timestamptz not null default now(),

  constraint scheduling_goal_synthetic_key
    primary key (id)
);

comment on table scheduling_goal is e''
  'A goal for scheduling of a plan.';
comment on column scheduling_goal.id is e''
  'The synthetic identifier for this scheduling goal.';
comment on column scheduling_goal.revision is e''
  'A monotonic clock that ticks for every change to this scheduling goal.';
comment on column scheduling_goal.definition is e''
  'The source code for a Typescript module defining this scheduling goal';
comment on column scheduling_goal.model_id is e''
  'The mission model used to which this scheduling goal is associated.';
comment on column scheduling_goal.name is e''
  'A short human readable name for this goal';
comment on column scheduling_goal.description is e''
  'A longer text description of this scheduling goal.';
comment on column scheduling_goal.author is e''
  'The original user who authored this scheduling goal.';
comment on column scheduling_goal.last_modified_by is e''
  'The last user who modified this scheduling goal.';
comment on column scheduling_goal.created_date is e''
  'The date this scheduling goal was created.';
comment on column scheduling_goal.modified_date is e''
  'The date this scheduling goal was last modified.';

create function update_logging_on_update_scheduling_goal()
  returns trigger
  security definer
language plpgsql as $$begin
  new.revision = old.revision + 1;
  new.modified_date = now();
return new;
end$$;

create trigger update_logging_on_update_scheduling_goal_trigger
  before update on scheduling_goal
  for each row
  when (pg_trigger_depth() < 1)
  execute function update_logging_on_update_scheduling_goal();

/*
DATA MIGRATION
*/
-- Goals not on a model spec will not be kept, as the scheduler DB can't get the model id from the plan id
-- Because multiple spec may be using the same goal/goal definition, we have to regenerate the id
with specified_definition(goal_id, goal_revision, model_id, definition, definition_creation) as (
 select gd.goal_id, gd.revision, s.model_id, gd.definition, gd.created_at
      from scheduling_model_specification_goals s
      left join scheduling_goal_definition gd using (goal_id)
      where ((s.goal_revision is not null and s.goal_revision = gd.revision)
      or (s.goal_revision is null and gd.revision = (select def.revision
                                                      from scheduling_goal_definition def
                                                      where def.goal_id = s.goal_id
                                                      order by def.revision desc limit 1)))
)
insert into scheduling_goal(revision, name, definition, model_id, description,
                            author, last_modified_by, created_date, modified_date)
select sd.goal_revision, m.name, sd.definition, sd.model_id, m.description,
       m.owner, m.updated_by, m.created_at, greatest(m.updated_at::timestamptz, sd.definition_creation::timestamptz)
  from scheduling_goal_metadata m
  inner join specified_definition sd on m.id = sd.goal_id;
/*
POST DATA MIGRATION TABLE CHANGES
*/
drop trigger set_timestamp on scheduling_goal_metadata;
drop function scheduling_goal_metadata_set_updated_at();

/*
SCHEDULING SPECIFICATION
*/
create function increment_revision_on_goal_update()
  returns trigger
  security definer
language plpgsql as $$begin
  with goals as (
    select g.specification_id from scheduling_specification_goals as g
    where g.goal_id = new.id
  )
  update scheduling_specification set revision = revision + 1
  where exists(select 1 from goals where specification_id = id);
  return new;
end$$;
create trigger increment_revision_on_goal_update
  before update on scheduling_goal
  for each row
  execute function increment_revision_on_goal_update();


/*
SPECIFICATIONS
*/
drop trigger increment_revision_on_goal_delete on scheduling_specification_goals;
drop function increment_spec_revision_on_goal_spec_delete();
drop trigger increment_revision_on_goal_update on scheduling_specification_goals;
drop function increment_spec_revision_on_goal_spec_update();

create or replace function delete_scheduling_specification_goal_func()
  returns trigger as $$begin
    update scheduling_specification_goals
      set priority = priority - 1
      where specification_id = OLD.specification_id
        and priority > OLD.priority;
    return null;
  end;$$
language plpgsql;

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

alter table scheduling_specification_goals
  add constraint scheduling_specification_unique_goal_id
    unique (goal_id),
  drop constraint scheduling_spec_goal_definition_exists,
  drop constraint scheduling_spec_goal_exists,
  add constraint scheduling_specification_goals_references_scheduling_goals
    foreign key (goal_id)
      references scheduling_goal
      on update cascade
      on delete cascade,
  drop constraint scheduling_specification_goals_specification_exists,
  add constraint scheduling_specification_goals_references_scheduling_specification
    foreign key (specification_id)
      references scheduling_specification
      on update cascade
      on delete cascade,
  alter column enabled drop not null,
  alter column priority set default null,
  drop column goal_revision;

comment on table scheduling_specification_goals is e''
  'A join table associating scheduling specifications with scheduling goals.';
comment on column scheduling_specification_goals.specification_id is e''
  'The ID of the scheduling specification a scheduling goal is associated with.';
comment on column scheduling_specification_goals.goal_id is e''
  'The ID of the scheduling goal a scheduling specification is associated with.';
comment on column scheduling_specification_goals.priority is e''
  'The relative priority of a scheduling goal in relation to other '
  'scheduling goals within the same specification.';
comment on column scheduling_specification_goals.enabled is null;
comment on column scheduling_specification_goals.simulate_after is e''
  'Whether to re-simulate after evaluating this goal and before the next goal.';

drop trigger delete_scheduling_model_specification_goal on scheduling_model_specification_goals;
drop function delete_scheduling_model_specification_goal_func();
drop trigger update_scheduling_model_specification_goal on scheduling_model_specification_goals;
drop function update_scheduling_model_specification_goal_func();
drop trigger insert_scheduling_model_specification_goal on scheduling_model_specification_goals;
drop function insert_scheduling_model_specification_goal_func();

drop table scheduling_model_specification_goals;
/*
TAGS
*/
drop table metadata.scheduling_goal_definition_tags;
alter table metadata.scheduling_goal_tags
drop constraint scheduling_goal_tags_goal_id_fkey,
add foreign key (goal_id) references public.scheduling_goal
    on update cascade
    on delete cascade;
/*
DEFINITION
*/
drop trigger scheduling_goal_definition_set_revision on scheduling_goal_definition;
drop function scheduling_goal_definition_set_revision();
drop table scheduling_goal_definition;
/*
METADATA
*/
drop index goal_name_unique_if_published;
drop table scheduling_goal_metadata;

/**********
SCHEDULING CONDITION
***********/
/*
RESTORE ORIGINAL
*/
create table scheduling_condition (
  id integer generated by default as identity,
  revision integer not null default 0,
  name text not null,
  definition text not null,

  model_id integer not null,
  description text not null default '',
  author text null,
  last_modified_by text null,
  created_date timestamptz not null default now(),
  modified_date timestamptz not null default now(),

  constraint scheduling_condition_synthetic_key
    primary key (id)
);

comment on table scheduling_condition is e''
  'A condition restricting scheduling of a plan.';
comment on column scheduling_condition.id is e''
  'The synthetic identifier for this scheduling condition.';
comment on column scheduling_condition.revision is e''
  'A monotonic clock that ticks for every change to this scheduling condition.';
comment on column scheduling_condition.definition is e''
  'The source code for a Typescript module defining this scheduling condition';
comment on column scheduling_condition.model_id is e''
  'The mission model used to which this scheduling condition is associated.';
comment on column scheduling_condition.name is e''
  'A short human readable name for this condition';
comment on column scheduling_condition.description is e''
  'A longer text description of this scheduling condition.';
comment on column scheduling_condition.author is e''
  'The original user who authored this scheduling condition.';
comment on column scheduling_condition.last_modified_by is e''
  'The last user who modified this scheduling condition.';
comment on column scheduling_condition.created_date is e''
  'The date this scheduling condition was created.';
comment on column scheduling_condition.modified_date is e''
  'The date this scheduling condition was last modified.';

create function update_logging_on_update_scheduling_condition()
  returns trigger
  security definer
language plpgsql as $$begin
  new.revision = old.revision + 1;
  new.modified_date = now();
return new;
end$$;

create trigger update_logging_on_update_scheduling_condition_trigger
  before update on scheduling_condition
  for each row
  when (pg_trigger_depth() < 1)
  execute function update_logging_on_update_scheduling_condition();

/*
DATA MIGRATION
*/
-- Conditions not on a model spec will not be kept, as the scheduler DB can't get the model id from the plan id
-- Because there is no uniqueness constraint on Scheduling Conditions when it comes to specifications, the ids can be preserved
with specified_definition(condition_id, condition_revision, model_id, definition, definition_creation) as (
 select cd.condition_id, cd.revision, s.model_id, cd.definition, cd.created_at
      from scheduling_model_specification_conditions s
      left join scheduling_condition_definition cd using (condition_id)
      where ((s.condition_revision is not null and s.condition_revision = cd.revision)
      or (s.condition_revision is null and cd.revision = (select def.revision
                                                      from scheduling_condition_definition def
                                                      where def.condition_id = s.condition_id
                                                      order by def.revision desc limit 1)))
)
insert into scheduling_condition(id, revision, name, definition, model_id, description,
                                 author, last_modified_by, created_date, modified_date)
select m.id, sd.condition_revision, m.name, sd.definition, sd.model_id, m.description,
       m.owner, m.updated_by, m.updated_at, greatest(m.updated_at::timestamptz, sd.definition_creation::timestamptz)
  from scheduling_condition_metadata m
  inner join specified_definition sd on m.id = sd.condition_id;

/*
POST DATA MIGRATION TABLE CHANGES
*/
drop trigger set_timestamp on scheduling_condition_metadata;
drop function scheduling_condition_metadata_set_updated_at();

alter table scheduling_condition
  alter column id set generated always;
/*
SPECIFICATIONS
*/
drop trigger increment_revision_on_condition_delete on scheduling_specification_conditions;
drop function increment_spec_revision_on_conditions_spec_delete();
drop trigger increment_revision_on_condition_update on scheduling_specification_conditions;
drop function increment_spec_revision_on_conditions_spec_update();

alter table scheduling_specification_conditions
  drop constraint scheduling_specification_condition_definition_exists,
  drop constraint scheduling_specification_condition_exists,
  add constraint scheduling_specification_conditions_references_scheduling_conditions
    foreign key (condition_id)
      references scheduling_condition
      on update cascade
      on delete cascade,
  drop constraint scheduling_specification_conditions_specification_exists,
  add constraint scheduling_specification_conditions_references_scheduling_specification
    foreign key (specification_id)
      references scheduling_specification
      on update cascade
      on delete cascade,
  drop column condition_revision;

comment on table scheduling_specification_conditions is e''
  'A join table associating scheduling specifications with scheduling conditions.';
comment on column scheduling_specification_conditions.specification_id is e''
  'The ID of the scheduling specification a scheduling goal is associated with.';
comment on column scheduling_specification_conditions.condition_id is e''
  'The ID of the condition a scheduling specification is associated with.';
comment on column scheduling_specification_conditions.enabled is null;

drop table scheduling_model_specification_conditions;

/*
TAGS
*/
drop table metadata.scheduling_condition_definition_tags;
drop table metadata.scheduling_condition_tags;

/*
DEFINITION
*/
drop trigger scheduling_goal_definition_set_revision on scheduling_condition_definition;
drop function scheduling_condition_definition_set_revision();
drop table scheduling_condition_definition;

/*
METADATA
*/
drop index condition_name_unique_if_published;
drop table scheduling_condition_metadata;

call migrations.mark_migration_rolled_back('13');
