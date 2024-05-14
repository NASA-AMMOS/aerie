begin;
-------------------------
-- INIT UTIL_FUNCTIONS --
-------------------------
create function util_functions.set_updated_at()
returns trigger
security invoker
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create function util_functions.increment_revision_update()
returns trigger
security invoker
language plpgsql as $$
begin
  new.revision = old.revision +1;
  return new;
end$$;

create function util_functions.raise_duration_is_negative()
returns trigger
security invoker
language plpgsql as $$begin
  raise exception 'invalid duration, expected nonnegative duration but found: %', new.duration;
end$$;

---------------------
-- UPDATE TRIGGERS --
---------------------
create or replace trigger set_timestamp
before update or insert on merlin.plan
for each row
execute function util_functions.set_updated_at();
drop function merlin.plan_set_updated_at();

create or replace trigger check_plan_duration_is_nonnegative_trigger
before insert or update on merlin.plan
for each row
when (new.duration < '0')
execute function util_functions.raise_duration_is_negative();
drop function merlin.raise_duration_is_negative();

drop trigger increment_revision_on_update_plan_trigger on merlin.plan;
create trigger increment_revision_plan_update
before update on merlin.plan
for each row
when (pg_trigger_depth() < 1)
execute function util_functions.increment_revision_update();
drop function merlin.increment_revision_on_update_plan();

drop trigger increment_revision_on_update_mission_model_trigger on merlin.mission_model;
create trigger increment_revision_mission_model_update
before update on merlin.mission_model
for each row
when (pg_trigger_depth() < 1)
execute function util_functions.increment_revision_update();
drop function merlin.increment_revision_on_update_mission_model();

alter trigger increment_revision_on_update_mission_model_jar_trigger on merlin.uploaded_file rename to increment_revision_mission_model_jar_update_trigger;

alter function merlin.create_dataset() rename to plan_dataset_create_dataset;
alter function merlin.process_delete() rename to plan_dataset_process_delete;

alter trigger increment_revision_on_insert_activity_directive_trigger on merlin.activity_directive rename to increment_plan_revision_on_directive_insert_trigger;
alter trigger increment_revision_on_update_activity_directive_trigger on merlin.activity_directive rename to increment_plan_revision_on_directive_update_trigger;
alter trigger increment_revision_on_delete_activity_directive_trigger on merlin.activity_directive rename to increment_plan_revision_on_directive_delete_trigger;

create or replace trigger activity_directive_metadata_schema_updated_at_trigger
before update on merlin.activity_directive_metadata_schema
for each row
execute procedure util_functions.set_updated_at();
drop function merlin.activity_directive_metadata_schema_updated_at();

create or replace trigger set_timestamp
before update on merlin.constraint_metadata
for each row
execute function util_functions.set_updated_at();
drop function merlin.constraint_metadata_set_updated_at();

create or replace trigger set_timestamp
  before update or insert on merlin.merge_request
  for each row
execute function util_functions.set_updated_at();
drop function merlin.merge_request_set_updated_at();

drop trigger increment_revision_for_update_simulation_trigger on merlin.simulation;
create trigger increment_revision_for_update_simulation_trigger
before update on merlin.simulation
for each row
when (pg_trigger_depth() < 1)
execute function util_functions.increment_revision_update();
drop function merlin.increment_revision_for_update_simulation();

drop trigger increment_revision_for_update_simulation_template_trigger on merlin.simulation_template;
create trigger increment_revision_for_update_simulation_template_trigger
before update on merlin.simulation_template
for each row
when (pg_trigger_depth() < 1)
execute function util_functions.increment_revision_update();
drop function merlin.increment_revision_for_update_simulation_template();

---------------------------------
-- UPDATE FUNCTION DEFINITIONS --
---------------------------------
create or replace function merlin.cleanup_on_delete()
  returns trigger
  language plpgsql as $$
begin
  -- prevent deletion if the plan is locked
  if old.is_locked then
    raise exception 'Cannot delete locked plan.';
  end if;

  -- withdraw pending rqs
  update merlin.merge_request
  set status='withdrawn'
  where plan_id_receiving_changes = old.id
    and status = 'pending';

  -- have the children be 'adopted' by this plan's parent
  update merlin.plan
  set parent_id = old.parent_id
  where
    parent_id = old.id;
  return old;
end
$$;

alter function merlin.increment_revision_on_update_mission_model_jar() rename to increment_revision_mission_model_jar_update;
create or replace function merlin.increment_revision_mission_model_jar_update()
returns trigger
security definer
language plpgsql as $$begin
  update merlin.mission_model
  set revision = revision + 1
  where jar_id = new.id
    or jar_id = old.id;

  return new;
end$$;

create or replace function merlin.plan_dataset_create_dataset()
returns trigger
security definer
language plpgsql as $$
begin
  insert into merlin.dataset
  default values
  returning id into new.dataset_id;
  return new;
end$$;

create or replace function merlin.calculate_offset()
returns trigger
security definer
language plpgsql as $$
declare
  reference merlin.plan_dataset;
  reference_plan_start timestamptz;
  dataset_start timestamptz;
  new_plan_start timestamptz;
begin
  -- Get an existing association with this dataset for reference
  select into reference * from merlin.plan_dataset
  where dataset_id = new.dataset_id;

  -- If no reference exists, raise an exception
  if reference is null
  then
    raise exception 'Nonexistent dataset_id --> %', new.dataset_id
          using hint = 'dataset_id must already be associated with a plan.';
  end if;

  -- Get the plan start times
  select start_time into reference_plan_start from merlin.plan where id = reference.plan_id;
  select start_time into new_plan_start from merlin.plan where id = new.plan_id;

  -- calculate and assign the new offset from plan start
  dataset_start := reference_plan_start + reference.offset_from_plan_start;
  new.offset_from_plan_start = dataset_start - new_plan_start;
  return new;
end$$;

create or replace function merlin.plan_dataset_process_delete()
returns trigger
security definer
language plpgsql as $$begin
  if (select count(*) from merlin.plan_dataset where dataset_id = old.dataset_id) = 0
  then
    delete from merlin.dataset
    where id = old.dataset_id;
  end if;
return old;
end$$;

create or replace function merlin.get_approximate_start_time(_activity_id int, _plan_id int)
  returns timestamptz
  security definer
  language plpgsql as $$
  declare
    _plan_duration interval;
    _plan_start_time timestamptz;
    _net_offset interval;
    _root_activity_id int;
    _root_anchored_to_start boolean;
begin
    -- Sum up all the activities from here until the plan
    with recursive get_net_offset(activity_id, plan_id, anchor_id, net_offset) as (
      select id, plan_id, anchor_id, start_offset
      from merlin.activity_directive ad
      where (ad.id, ad.plan_id) = (_activity_id, _plan_id)
      union
      select ad.id, ad.plan_id, ad.anchor_id, ad.start_offset+gno.net_offset
      from merlin.activity_directive ad, get_net_offset gno
      where (ad.id, ad.plan_id) = (gno.anchor_id, gno.plan_id)
    )
    select gno.net_offset, activity_id from get_net_offset gno
    where gno.anchor_id is null
    into _net_offset, _root_activity_id;

  -- Get the plan start time and duration
  select start_time, duration
  from merlin.plan
  where id = _plan_id
  into _plan_start_time, _plan_duration;

  select anchored_to_start
  from merlin.activity_directive
  where (id, plan_id) = (_root_activity_id, _plan_id)
  into _root_anchored_to_start;

  -- If the root activity is anchored to the end of the plan, add the net to duration
  if not _root_anchored_to_start then
    _net_offset = _plan_duration + _net_offset;
  end if;

  return _plan_start_time+_net_offset;
end
$$;

alter function merlin.increment_revision_on_insert_activity_directive() rename to increment_plan_revision_on_directive_insert;
create or replace function merlin.increment_plan_revision_on_directive_insert()
returns trigger
security definer
language plpgsql as $$begin
  update merlin.plan
  set revision = revision + 1
  where id = new.plan_id;

  return new;
end$$;

alter function merlin.increment_revision_on_update_activity_directive() rename to increment_plan_revision_on_directive_update;
create or replace function merlin.increment_plan_revision_on_directive_update()
returns trigger
security definer
language plpgsql as $$begin
  update merlin.plan
  set revision = revision + 1
  where id = new.plan_id
    or id = old.plan_id;

  return new;
end$$;

alter function merlin.increment_revision_on_delete_activity_directive() rename to increment_plan_revision_on_directive_delete;
create or replace function merlin.increment_plan_revision_on_directive_delete()
returns trigger
security invoker
language plpgsql as $$begin
  update merlin.plan
  set revision = revision + 1
  where id = old.plan_id;

  return old;
end$$;

create or replace function merlin.generate_activity_directive_name()
returns trigger
security invoker
language plpgsql as $$begin
  call merlin.plan_locked_exception(new.plan_id);
  if new.name is null then
    new.name = new.type || ' ' || new.id;
  end if;
  return new;
end$$;

alter function merlin.activity_directive_set_updated_at() rename to set_last_modified_at;
create or replace function merlin.set_last_modified_at()
returns trigger
security invoker
language plpgsql as $$begin
  new.last_modified_at = now();
  return new;
end$$;

create or replace function merlin.activity_directive_set_arguments_updated_at()
  returns trigger
  security definer
  language plpgsql as
$$ begin
  call merlin.plan_locked_exception(new.plan_id);
  new.last_modified_arguments_at = now();

  -- request new validation
  update merlin.activity_directive_validations
    set last_modified_arguments_at = new.last_modified_arguments_at,
        status = 'pending'
    where (directive_id, plan_id) = (new.id, new.plan_id);

  return new;
end $$;

create or replace function merlin.activity_directive_validation_entry()
  returns trigger
  security definer
  language plpgsql as
$$ begin
  insert into merlin.activity_directive_validations
    (directive_id, plan_id, last_modified_arguments_at)
    values (new.id, new.plan_id, new.last_modified_arguments_at);
  return new;
end $$;

create or replace function merlin.check_activity_directive_metadata()
returns trigger
security definer
language plpgsql as $$
  declare
    _key text;
    _value jsonb;
    _schema jsonb;
    _type text;
    _subValue jsonb;
  begin
  call merlin.plan_locked_exception(new.plan_id);
  for _key, _value in
    select * from jsonb_each(new.metadata::jsonb)
  loop
    select schema into _schema from merlin.activity_directive_metadata_schema where key = _key;
    _type := _schema->>'type';
    if _type = 'string' then
      if jsonb_typeof(_value) != 'string' then
        raise exception 'invalid metadata value for key %. Expected: string, Received: %', _key, _value;
      end if;
    elsif _type = 'long_string' then
      if jsonb_typeof(_value) != 'string' then
        raise exception 'invalid metadata value for key %. Expected: string, Received: %', _key, _value;
      end if;
    elsif _type = 'boolean' then
      if jsonb_typeof(_value) != 'boolean' then
        raise exception 'invalid metadata value for key %. Expected: boolean, Received: %', _key, _value;
      end if;
    elsif _type = 'number' then
      if jsonb_typeof(_value) != 'number' then
        raise exception 'invalid metadata value for key %. Expected: number, Received: %', _key, _value;
      end if;
    elsif _type = 'enum' then
      if (_value not in (select * from jsonb_array_elements(_schema->'enumerates'))) then
        raise exception 'invalid metadata value for key %. Expected: %, Received: %', _key, _schema->>'enumerates', _value;
      end if;
    elsif _type = 'enum_multiselect' then
      if jsonb_typeof(_value) != 'array' then
        raise exception 'invalid metadata value for key %. Expected an array of enumerates: %, Received: %', _key, _schema->>'enumerates', _value;
      end if;
      for _subValue in select * from jsonb_array_elements(_value)
        loop
          if (_subValue not in (select * from jsonb_array_elements(_schema->'enumerates'))) then
            raise exception 'invalid metadata value for key %. Expected one of the valid enumerates: %, Received: %', _key, _schema->>'enumerates', _value;
          end if;
        end loop;
    end if;
  end loop;
  return new;
end$$;

create or replace function merlin.check_locked_on_delete()
  returns trigger
  security definer
  language plpgsql as $$
  begin
    call merlin.plan_locked_exception(old.plan_id);
    return old;
  end $$;

create or replace function merlin.store_activity_directive_change()
  returns trigger
  language plpgsql as $$
begin
  insert into merlin.activity_directive_changelog (
    revision,
    plan_id,
    activity_directive_id,
    name,
    start_offset,
    type,
    arguments,
    changed_arguments_at,
    metadata,
    changed_by,
    anchor_id,
    anchored_to_start)
  values (
    (select coalesce(max(revision), -1) + 1
     from merlin.activity_directive_changelog
     where plan_id = new.plan_id
      and activity_directive_id = new.id),
    new.plan_id,
    new.id,
    new.name,
    new.start_offset,
    new.type,
    new.arguments,
    new.last_modified_arguments_at,
    new.metadata,
    new.last_modified_by,
    new.anchor_id,
    new.anchored_to_start);

  return new;
end
$$;

create or replace function merlin.delete_min_activity_directive_revision()
  returns trigger
  language plpgsql as $$
begin
  delete from merlin.activity_directive_changelog
  where activity_directive_id = new.activity_directive_id
    and plan_id = new.plan_id
    and revision = (select min(revision)
                    from merlin.activity_directive_changelog
                    where activity_directive_id = new.activity_directive_id
                      and plan_id = new.plan_id);
  return new;
end$$;

create or replace function merlin.get_dependent_activities(_activity_id int, _plan_id int)
  returns table(activity_id int, total_offset interval)
  stable
  language plpgsql as $$
begin
  return query
  with recursive d_activities(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
      select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, ad.start_offset
      from merlin.activity_directive ad
      where (ad.anchor_id, ad.plan_id) = (_activity_id, _plan_id) -- select all activities anchored to this one
    union
      select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, da.total_offset + ad.start_offset
      from merlin.activity_directive ad, d_activities da
      where (ad.anchor_id, ad.plan_id) = (da.activity_id, _plan_id) -- select all activities anchored to those in the selection
        and ad.anchored_to_start  -- stop at next end-time anchor
  ) select da.activity_id, da.total_offset
  from d_activities da;
end;
$$;

create or replace procedure merlin.validate_nonnegative_net_end_offset(_activity_id integer, _plan_id integer)
  security definer
  language plpgsql as $$
declare
  end_anchor_id integer;
  offset_from_end_anchor interval;
  _anchor_id integer;
  _start_offset interval;
  _anchored_to_start boolean;
begin
  select anchor_id, start_offset, anchored_to_start
  from merlin.activity_directive
  where (id, plan_id) = (_activity_id, _plan_id)
  into _anchor_id, _start_offset, _anchored_to_start;

  if (_anchor_id is not null)           -- if the activity is anchored to the plan, then it can't be anchored to the end of another activity directive
  then
    /*
      Postgres ANDs don't "short-circuit" -- all clauses are evaluated. Therefore, this query is placed here so that
      it only runs iff the outer 'if' is true
    */
    with recursive end_time_anchor(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
      select _activity_id, _anchor_id, _anchored_to_start, _start_offset, _start_offset
      union
      select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, eta.total_offset + ad.start_offset
      from merlin.activity_directive ad, end_time_anchor eta
      where (ad.id, ad.plan_id) = (eta.anchor_id, _plan_id)
        and eta.anchor_id is not null                               -- stop at plan
        and eta.anchored_to_start                                   -- or stop at end time anchor
    ) select into end_anchor_id, offset_from_end_anchor
        anchor_id, total_offset from end_time_anchor eta -- get the id of the activity that the selected activity is anchored to
    where not eta.anchored_to_start and eta.anchor_id is not null
    limit 1;

    if end_anchor_id is not null and offset_from_end_anchor < '0' then
      raise notice 'Activity Directive % has a net negative offset relative to an end-time anchor on Activity Directive %.', _activity_id, end_anchor_id;

      insert into merlin.anchor_validation_status (activity_id, plan_id, reason_invalid)
      values (_activity_id, _plan_id, 'Activity Directive ' || _activity_id || ' has a net negative offset relative to an end-time' ||
                                      ' anchor on Activity Directive ' || end_anchor_id ||'.')
      on conflict (activity_id, plan_id) do update
        set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to an end-time' ||
                             ' anchor on Activity Directive ' || end_anchor_id ||'.';
    end if;
  end if;
end
$$;

create or replace procedure merlin.validate_nonegative_net_plan_start(_activity_id integer, _plan_id integer)
  security definer
  language plpgsql as $$
  declare
    net_offset interval;
    _anchor_id integer;
    _start_offset interval;
    _anchored_to_start boolean;
  begin
    select anchor_id, start_offset, anchored_to_start
    from merlin.activity_directive
    where (id, plan_id) = (_activity_id, _plan_id)
    into _anchor_id, _start_offset, _anchored_to_start;

    if (_start_offset < '0' and _anchored_to_start) then -- only need to check if anchored to start or something with a negative offset
      with recursive anchors(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
          select _activity_id, _anchor_id, _anchored_to_start, _start_offset, _start_offset
        union
          select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, anchors.total_offset + ad.start_offset
          from merlin.activity_directive ad, anchors
          where anchors.anchor_id is not null                               -- stop at plan
            and  (ad.id, ad.plan_id) = (anchors.anchor_id, _plan_id)
            and anchors.anchored_to_start                                  -- or, stop at end-time offset
      )
      select total_offset  -- get the id of the activity that the selected activity is anchored to
      from anchors a
      where a.anchor_id is null
        and a.anchored_to_start
      limit 1
      into net_offset;

      if(net_offset < '0') then
        raise notice 'Activity Directive % has a net negative offset relative to Plan Start.', _activity_id;

        insert into merlin.anchor_validation_status (activity_id, plan_id, reason_invalid)
        values (_activity_id, _plan_id, 'Activity Directive ' || _activity_id || ' has a net negative offset relative to Plan Start.')
        on conflict (activity_id, plan_id) do update
          set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to Plan Start.';
     end if;
    end if;
    end
  $$;

create or replace function merlin.validate_anchors()
  returns trigger
  security definer
  language plpgsql as $$
declare
  end_anchor_id integer;
  invalid_descendant_act_ids integer[];
  offset_from_end_anchor interval;
  offset_from_plan_start interval;
begin
  -- Clear the reason invalid field (if an exception is thrown, this will be rolled back)
  insert into merlin.anchor_validation_status (activity_id, plan_id, reason_invalid)
  values (new.id, new.plan_id, '')
  on conflict (activity_id, plan_id) do update
    set reason_invalid = '';

  -- An activity cannot anchor to itself
  if(new.anchor_id = new.id) then
    raise exception 'Cannot anchor activity % to itself.', new.anchor_id;
  end if;

  -- Validate that no cycles were added
  if exists(
      with recursive history(activity_id, anchor_id, is_cycle, path) as (
        select new.id, new.anchor_id, false, array[new.id]
        union all
        select ad.id, ad.anchor_id,
               ad.id = any(path),
               path || ad.id
        from merlin.activity_directive ad, history h
        where (ad.id, ad.plan_id) = (h.anchor_id, new.plan_id)
          and not is_cycle
      ) select * from history
      where is_cycle
      limit 1
    ) then
    raise exception 'Cycle detected. Cannot apply changes.';
  end if;

  /*
    An activity directive may have a negative offset from its anchor's start time.
    If its anchor is anchored to the end time of another activity (or so on up the chain), the activity with a
    negative offset must come out to have a positive offset relative to that end time anchor.
  */
  call merlin.validate_nonnegative_net_end_offset(new.id, new.plan_id);
  call merlin.validate_nonegative_net_plan_start(new.id, new.plan_id);

  /*
    Everything below validates that the activities anchored to this one did not become invalid as a result of these changes.

    This only checks descendent start-time anchors, as we know that the state after an end-time anchor is valid
    (As if it no longer is, it will be caught when that activity's row is processed by this trigger)
  */
  -- Get the total offset from the most recent end-time anchor earlier in this activity's chain (or null if there is none)
  with recursive end_time_anchor(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
    select new.id, new.anchor_id, new.anchored_to_start, new.start_offset, new.start_offset
    union
    select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, eta.total_offset + ad.start_offset
    from merlin.activity_directive ad, end_time_anchor eta
    where (ad.id, ad.plan_id) = (eta.anchor_id, new.plan_id)
      and eta.anchor_id is not null                               -- stop at plan
      and eta.anchored_to_start                                   -- or stop at end time anchor
  ) select into end_anchor_id, offset_from_end_anchor
        anchor_id, total_offset from end_time_anchor eta -- get the id of the activity that the selected activity is anchored to
  where not eta.anchored_to_start and eta.anchor_id is not null
  limit 1;

  -- Not null iff the activity being looked at has some end anchor to another activity in its chain
  if offset_from_end_anchor is not null then
    select array_agg(activity_id)
    from merlin.get_dependent_activities(new.id, new.plan_id)
    where total_offset + offset_from_end_anchor < '0'
    into invalid_descendant_act_ids;

    if invalid_descendant_act_ids is not null then
      raise info 'The following Activity Directives now have a net negative offset relative to an end-time anchor on Activity Directive %: % \n'
        'There may be additional activities that are invalid relative to this activity.',
        end_anchor_id, array_to_string(invalid_descendant_act_ids, ',');

      insert into merlin.anchor_validation_status (activity_id, plan_id, reason_invalid)
      select id, new.plan_id, 'Activity Directive ' || id || ' has a net negative offset relative to an end-time' ||
                              ' anchor on Activity Directive ' || end_anchor_id ||'.'
      from unnest(invalid_descendant_act_ids) as id
      on conflict (activity_id, plan_id) do update
      set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to an end-time' ||
                           ' anchor on Activity Directive ' || end_anchor_id ||'.';
    end if;
  end if;

  -- Gets the total offset from plan start (or null if there's an end-time anchor in the way)
  with recursive anchors(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
    select new.id, new.anchor_id, new.anchored_to_start, new.start_offset, new.start_offset
    union
    select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, anchors.total_offset + ad.start_offset
    from merlin.activity_directive ad, anchors
    where anchors.anchor_id is not null                               -- stop at plan
      and (ad.id, ad.plan_id) = (anchors.anchor_id, new.plan_id)
      and anchors.anchored_to_start                                  -- or, stop at end-time offset
  )
  select total_offset  -- get the id of the activity that the selected activity is anchored to
  from anchors a
  where a.anchor_id is null
    and a.anchored_to_start
  limit 1
  into offset_from_plan_start;

  -- Not null iff the activity being looked at is connected to plan start via a chain of start anchors
  if offset_from_plan_start is not null then
    -- Validate descendents
    invalid_descendant_act_ids := null;
    select array_agg(activity_id)
    from merlin.get_dependent_activities(new.id, new.plan_id)
    where total_offset + offset_from_plan_start < '0'
    into invalid_descendant_act_ids;  -- grab all and split

    if invalid_descendant_act_ids is not null then
      raise info 'The following Activity Directives now have a net negative offset relative to Plan Start: % \n'
        'There may be additional activities that are invalid relative to this activity.',
        array_to_string(invalid_descendant_act_ids, ',');

      insert into merlin.anchor_validation_status (activity_id, plan_id, reason_invalid)
      select id, new.plan_id, 'Activity Directive ' || id || ' has a net negative offset relative to Plan Start.'
      from unnest(invalid_descendant_act_ids) as id
      on conflict (activity_id, plan_id) do update
        set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to Plan Start.';
    end if;
  end if;

  -- These are both null iff the activity is anchored to plan end
  if(offset_from_plan_start is null and offset_from_end_anchor is null) then
    -- All dependent activities should have no errors, as Plan End can have an offset of any value.
    insert into merlin.anchor_validation_status (activity_id, plan_id, reason_invalid)
    select da.activity_id, new.plan_id, ''
    from merlin.get_dependent_activities(new.id, new.plan_id) as da
    on conflict (activity_id, plan_id) do update
      set reason_invalid = '';
  end if;

  -- Remove the error from the dependent activities that wouldn't have been flagged by the earlier checks.
  insert into merlin.anchor_validation_status (activity_id, plan_id, reason_invalid)
  select da.activity_id, new.plan_id, ''
  from merlin.get_dependent_activities(new.id, new.plan_id) as da
  where total_offset + offset_from_plan_start >= '0'
    or total_offset + offset_from_end_anchor >= '0' -- only one of these checks will run depending on which one has `null` behind the offset
  on conflict (activity_id, plan_id) do update
    set reason_invalid = '';

  return new;
end $$;

create or replace function merlin.constraint_definition_set_revision()
returns trigger
volatile
language plpgsql as $$
declare
  max_revision integer;
begin
  -- Grab the current max value of revision, or -1, if this is the first revision
  select coalesce((select revision
  from merlin.constraint_definition
  where constraint_id = new.constraint_id
  order by revision desc
  limit 1), -1)
  into max_revision;

  new.revision = max_revision + 1;
  return new;
end
$$;

create or replace function merlin.delete_dataset_cascade()
  returns trigger
  security definer
  language plpgsql as
$$begin
  delete from merlin.span s where s.dataset_id = old.id;
  return old;
end$$;

create or replace function merlin.allocate_dataset_partitions(dataset_id integer)
  returns merlin.dataset
  security definer
  language plpgsql as $$
declare
  dataset_ref merlin.dataset;
begin
  select * from merlin.dataset d where d.id = dataset_id into dataset_ref;
  if dataset_id is null
  then
    raise exception 'Cannot allocate partitions for non-existent dataset id %', dataset_id;
  end if;

  execute 'create table merlin.profile_segment_' || dataset_id || ' (
    like merlin.profile_segment including defaults including constraints
  );';
  execute 'alter table merlin.profile_segment
    attach partition merlin.profile_segment_' || dataset_id || ' for values in ('|| dataset_id ||');';

  execute 'create table merlin.event_' || dataset_id || ' (
      like merlin.event including defaults including constraints
    );';
  execute 'alter table merlin.event
    attach partition merlin.event_' || dataset_id || ' for values in (' || dataset_id || ');';

  execute 'create table merlin.span_' || dataset_id || ' (
       like merlin.span including defaults including constraints
    );';
  execute 'alter table merlin.span
    attach partition merlin.span_' || dataset_id || ' for values in (' || dataset_id || ');';

  -- Create a self-referencing foreign key on the span partition table. We avoid referring to the top level span table
  -- in order to avoid lock contention with concurrent inserts
  call merlin.span_add_foreign_key_to_partition('merlin.span_' || dataset_id);
  return dataset_ref;
end$$;

create or replace function merlin.call_create_partition()
  returns trigger
  security invoker
  language plpgsql as $$
begin
  perform merlin.allocate_dataset_partitions(new.id);
  return new;
end
$$;

create or replace function merlin.event_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(
    select from merlin.topic t
      where t.dataset_id = new.dataset_id
      and t.topic_index = new.topic_index
    for key share of t)
    -- for key share is important: it makes sure that concurrent transactions cannot update
    -- the columns that compose the topic's key until after this transaction commits.
  then
    raise exception 'foreign key violation: there is no topic with topic_index % in dataset %', new.topic_index, new.dataset_id;
  end if;
  return new;
end$$;

create or replace function merlin.delete_profile_cascade()
  returns trigger
  security invoker
  language plpgsql as
$$begin
  delete from merlin.profile_segment ps
  where ps.dataset_id = old.dataset_id and ps.profile_id = old.id;
  return old;
end$$;

create or replace function merlin.update_profile_cascade()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if old.id != new.id or old.dataset_id != new.dataset_id
  then
    update merlin.profile_segment ps
    set profile_id = new.id,
        dataset_id = new.dataset_id
    where ps.dataset_id = old.dataset_id and ps.profile_id = old.id;
  end if;
  return new;
end$$;

create or replace function merlin.profile_segment_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(
    select from merlin.profile p
      where p.dataset_id = new.dataset_id
      and p.id = new.profile_id
    for key share of p)
    -- for key share is important: it makes sure that concurrent transactions cannot update
    -- the columns that compose the profile's key until after this transaction commits.
  then
    raise exception 'foreign key violation: there is no profile with id % in dataset %', new.profile_id, new.dataset_id;
  end if;
  return new;
end$$;

create or replace function merlin.span_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(select from merlin.dataset d where d.id = new.dataset_id for key share of d)
  then
    raise exception 'foreign key violation: there is no dataset with id %', new.dataset_id;
  end if;
  return new;
end$$;

create or replace function merlin.delete_topic_cascade()
  returns trigger
  security invoker
  language plpgsql as $$
begin
  delete from merlin.event e
  where e.topic_index = old.topic_index and e.dataset_id = old.dataset_id;
  return old;
end
$$;

create or replace function merlin.update_topic_cascade()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if old.topic_index != new.topic_index or old.dataset_id != new.dataset_id
  then
    update merlin.event e
    set topic_index = new.topic_index,
        dataset_id = new.dataset_id
    where e.dataset_id = old.dataset_id and e.topic_index = old.topic_index;
  end if;
  return new;
end$$;

create or replace function merlin.set_revisions_and_initialize_dataset_on_insert()
returns trigger
security definer
language plpgsql as $$
declare
  simulation_ref merlin.simulation;
  plan_ref merlin.plan;
  model_ref merlin.mission_model;
  template_ref merlin.simulation_template;
  dataset_ref merlin.dataset;
begin
  -- Set the revisions
  select into simulation_ref * from merlin.simulation where id = new.simulation_id;
  select into plan_ref * from merlin.plan where id = simulation_ref.plan_id;
  select into template_ref * from merlin.simulation_template where id = simulation_ref.simulation_template_id;
  select into model_ref * from merlin.mission_model where id = plan_ref.model_id;
  new.model_revision = model_ref.revision;
  new.plan_revision = plan_ref.revision;
  new.simulation_template_revision = template_ref.revision;
  new.simulation_revision = simulation_ref.revision;

  -- Create the dataset
  insert into merlin.dataset
  default values
  returning * into dataset_ref;
  new.dataset_id = dataset_ref.id;
  new.dataset_revision = dataset_ref.revision;
return new;
end$$;

create or replace function merlin.delete_dataset_on_delete()
returns trigger
security definer
language plpgsql as $$begin
  delete from merlin.dataset
  where id = old.dataset_id;
return old;
end$$;

create or replace function merlin.notify_simulation_workers()
returns trigger
security definer
language plpgsql as $$
declare
  simulation_ref merlin.simulation;
begin
  select into simulation_ref * from merlin.simulation where id = new.simulation_id;

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

create or replace function merlin.update_offset_from_plan_start()
returns trigger
security invoker
language plpgsql as $$
declare
  plan_start timestamptz;
begin
  select p.start_time
  from merlin.simulation s, merlin.plan p
  where s.plan_id = p.id
    and new.simulation_id = s.id
  into plan_start;

  new.offset_from_plan_start = new.simulation_start_time - plan_start;
  return new;
end
$$;

create or replace function merlin.anchor_direct_descendents_to_plan(_activity_id int, _plan_id int)
  returns setof merlin.activity_directive
  language plpgsql as $$
declare
  _total_offset interval;
begin
  if _plan_id is null then
    raise exception 'Plan ID cannot be null.';
  end if;
  if _activity_id is null then
    raise exception 'Activity ID cannot be null.';
  end if;
  if not exists(select id from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

  with recursive history(activity_id, anchor_id, total_offset) as (
    select ad.id, ad.anchor_id, ad.start_offset
    from merlin.activity_directive ad
    where (ad.id, ad.plan_id) = (_activity_id, _plan_id)
    union
    select ad.id, ad.anchor_id, h.total_offset + ad.start_offset
    from merlin.activity_directive ad, history h
    where (ad.id, ad.plan_id) = (h.anchor_id, _plan_id)
      and h.anchor_id is not null
  ) select total_offset
    from history
    where history.anchor_id is null
    into _total_offset;

  return query update merlin.activity_directive
    set start_offset = start_offset + _total_offset,
        anchor_id = null,
        anchored_to_start = true
    where (anchor_id, plan_id) = (_activity_id, _plan_id)
    returning *;
end
$$;

create or replace function merlin.anchor_direct_descendents_to_ancestor(_activity_id int, _plan_id int)
  returns setof merlin.activity_directive
  language plpgsql as $$
declare
  _current_offset interval;
  _current_anchor_id int;
begin
  if _plan_id is null then
    raise exception 'Plan ID cannot be null.';
  end if;
  if _activity_id is null then
    raise exception 'Activity ID cannot be null.';
  end if;
  if not exists(select id from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

  select start_offset, anchor_id
  from merlin.activity_directive
  where (id, plan_id) = (_activity_id, _plan_id)
  into _current_offset, _current_anchor_id;

  return query
    update merlin.activity_directive
    set start_offset = start_offset + _current_offset,
      anchor_id = _current_anchor_id
    where (anchor_id, plan_id) = (_activity_id, _plan_id)
    returning *;
end
$$;

create or replace function merlin.create_snapshot(_plan_id integer)
	returns integer
	language plpgsql as $$
begin
	return merlin.create_snapshot(_plan_id, null, null, null);
end
$$;

create or replace function merlin.create_snapshot(_plan_id integer, _snapshot_name text, _description text, _user text)
  returns integer -- snapshot id inserted into the table
  language plpgsql as $$
  declare
    validate_plan_id integer;
    inserted_snapshot_id integer;
begin
  select id from merlin.plan where plan.id = _plan_id into validate_plan_id;
  if validate_plan_id is null then
    raise exception 'Plan % does not exist.', _plan_id;
  end if;

  insert into merlin.plan_snapshot(plan_id, revision, snapshot_name, description, taken_by)
    select id, revision, _snapshot_name, _description, _user
    from merlin.plan where id = _plan_id
    returning snapshot_id into inserted_snapshot_id;
  insert into merlin.plan_snapshot_activities(
      snapshot_id, id, name, source_scheduling_goal_id, created_at, created_by,
      last_modified_at, last_modified_by, start_offset, type,
      arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      inserted_snapshot_id,                              -- this is the snapshot id
      id, name, source_scheduling_goal_id, created_at, created_by, -- these are the rest of the data for an activity row
      last_modified_at, last_modified_by, start_offset, type,
      arguments, last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from merlin.activity_directive where activity_directive.plan_id = _plan_id;
  insert into merlin.preset_to_snapshot_directive(preset_id, activity_id, snapshot_id)
    select ptd.preset_id, ptd.activity_id, inserted_snapshot_id
    from merlin.preset_to_directive ptd
    where ptd.plan_id = _plan_id;
  insert into tags.snapshot_activity_tags(snapshot_id, directive_id, tag_id)
    select inserted_snapshot_id, directive_id, tag_id
    from tags.activity_directive_tags adt
    where adt.plan_id = _plan_id;

  --all snapshots in plan_latest_snapshot for plan plan_id become the parent of the current snapshot
  insert into merlin.plan_snapshot_parent(snapshot_id, parent_snapshot_id)
    select inserted_snapshot_id, snapshot_id
    from merlin.plan_latest_snapshot where plan_latest_snapshot.plan_id = _plan_id;

  --remove all of those entries from plan_latest_snapshot and add this new snapshot.
  delete from merlin.plan_latest_snapshot where plan_latest_snapshot.plan_id = _plan_id;
  insert into merlin.plan_latest_snapshot(plan_id, snapshot_id) values (_plan_id, inserted_snapshot_id);

  return inserted_snapshot_id;
  end;
$$;

create or replace function merlin.get_plan_history(starting_plan_id integer)
  returns setof integer
  language plpgsql as $$
  declare
    validate_id integer;
  begin
    select plan.id from merlin.plan where plan.id = starting_plan_id into validate_id;
    if validate_id is null then
      raise exception 'Plan ID % is not present in plan table.', starting_plan_id;
    end if;

    return query with recursive history(id) as (
        values(starting_plan_id)                      -- base case
      union
        select parent_id from merlin.plan p
          join history on history.id = p.id and p.parent_id is not null-- recursive case
    ) select * from history;
  end
$$;

create or replace function merlin.get_snapshot_history_from_plan(starting_plan_id integer)
  returns setof integer
  language plpgsql as $$
  begin
    return query
      select merlin.get_snapshot_history(snapshot_id)  --runs the recursion
      from merlin.plan_latest_snapshot where plan_id = starting_plan_id; --supplies input for get_snapshot_history
  end
$$;

create or replace function merlin.get_snapshot_history(starting_snapshot_id integer)
  returns setof integer
  language plpgsql as $$
  declare
    validate_id integer;
begin
  select plan_snapshot.snapshot_id from merlin.plan_snapshot where plan_snapshot.snapshot_id = starting_snapshot_id into validate_id;
  if validate_id is null then
    raise exception 'Snapshot ID % is not present in plan_snapshot table.', starting_snapshot_id;
  end if;

  return query with recursive history(id) as (
      values(starting_snapshot_id) --base case
    union
      select parent_snapshot_id from merlin.plan_snapshot_parent psp
        join history on id = psp.snapshot_id --recursive case
  ) select * from history;
end
$$;

create or replace procedure merlin.restore_from_snapshot(_plan_id integer, _snapshot_id integer)
	language plpgsql as $$
	declare
		_snapshot_name text;
		_plan_name text;
	begin
		-- Input Validation
		select name from merlin.plan where id = _plan_id into _plan_name;
		if _plan_name is null then
			raise exception 'Cannot Restore: Plan with ID % does not exist.', _plan_id;
		end if;
		if not exists(select snapshot_id from merlin.plan_snapshot where snapshot_id = _snapshot_id) then
			raise exception 'Cannot Restore: Snapshot with ID % does not exist.', _snapshot_id;
		end if;
		if not exists(select snapshot_id from merlin.plan_snapshot where _snapshot_id = snapshot_id and _plan_id = plan_id ) then
			select snapshot_name from merlin.plan_snapshot where snapshot_id = _snapshot_id into _snapshot_name;
			if _snapshot_name is not null then
        raise exception 'Cannot Restore: Snapshot ''%'' (ID %) is not a snapshot of Plan ''%'' (ID %)',
          _snapshot_name, _snapshot_id, _plan_name, _plan_id;
      else
				raise exception 'Cannot Restore: Snapshot % is not a snapshot of Plan ''%'' (ID %)',
          _snapshot_id, _plan_name, _plan_id;
      end if;
    end if;

		-- Catch Plan_Locked
		call merlin.plan_locked_exception(_plan_id);

    -- Record the Union of Activities in Plan and Snapshot
    -- and note which ones have been added since the Snapshot was taken (in_snapshot = false)
    create temp table diff(
			activity_id integer,
			in_snapshot boolean not null
		);
		insert into diff(activity_id, in_snapshot)
		select id as activity_id, true
		from merlin.plan_snapshot_activities where snapshot_id = _snapshot_id;

		insert into diff (activity_id, in_snapshot)
		select activity_id, false
		from(
				select id as activity_id
				from merlin.activity_directive
				where plan_id = _plan_id
			except
				select activity_id
				from diff) a;

		-- Remove any added activities
  delete from merlin.activity_directive ad
		using diff d
		where (ad.id, ad.plan_id) = (d.activity_id, _plan_id)
			and d.in_snapshot is false;

		-- Upsert the rest
		insert into merlin.activity_directive (
		      id, plan_id, name, source_scheduling_goal_id, created_at, created_by, last_modified_at, last_modified_by,
		      start_offset, type, arguments, last_modified_arguments_at, metadata,
		      anchor_id, anchored_to_start)
		select psa.id, _plan_id, psa.name, psa.source_scheduling_goal_id, psa.created_at, psa.created_by, psa.last_modified_at, psa.last_modified_by,
		       psa.start_offset, psa.type, psa.arguments, psa.last_modified_arguments_at, psa.metadata,
		       psa.anchor_id, psa.anchored_to_start
		from merlin.plan_snapshot_activities psa
		where psa.snapshot_id = _snapshot_id
		on conflict (id, plan_id) do update
		-- 'last_modified_at' and 'last_modified_arguments_at' are skipped during update, as triggers will overwrite them to now()
		set name = excluded.name,
		    source_scheduling_goal_id = excluded.source_scheduling_goal_id,
		    created_at = excluded.created_at,
		    created_by = excluded.created_by,
		    last_modified_by = excluded.last_modified_by,
		    start_offset = excluded.start_offset,
		    type = excluded.type,
		    arguments = excluded.arguments,
		    metadata = excluded.metadata,
		    anchor_id = excluded.anchor_id,
		    anchored_to_start = excluded.anchored_to_start;

		-- Tags
		delete from tags.activity_directive_tags adt
		using diff d
		where (adt.directive_id, adt.plan_id) = (d.activity_id, _plan_id);

		insert into tags.activity_directive_tags(directive_id, plan_id, tag_id)
		select sat.directive_id, _plan_id, sat.tag_id
		from tags.snapshot_activity_tags sat
		where sat.snapshot_id = _snapshot_id
		on conflict (directive_id, plan_id, tag_id) do nothing;

		-- Presets
		delete from merlin.preset_to_directive
		  where plan_id = _plan_id;
		insert into merlin.preset_to_directive(preset_id, activity_id, plan_id)
			select pts.preset_id, pts.activity_id, _plan_id
			from merlin.preset_to_snapshot_directive pts
			where pts.snapshot_id = _snapshot_id
			on conflict (activity_id, plan_id)
			do update	set preset_id = excluded.preset_id;

		-- Clean up
		drop table diff;
  end
$$;

create or replace procedure merlin.begin_merge(_merge_request_id integer, review_username text)
  language plpgsql as $$
  declare
    validate_id integer;
    validate_status merlin.merge_request_status;
    validate_non_no_op_status merlin.activity_change_type;
    snapshot_id_supplying integer;
    plan_id_receiving integer;
    merge_base_id integer;
begin
  -- validate id and status
  select id, status
    from merlin.merge_request
    where _merge_request_id = id
    into validate_id, validate_status;

  if validate_id is null then
    raise exception 'Request ID % is not present in merge_request table.', _merge_request_id;
  end if;

  if validate_status != 'pending' then
    raise exception 'Cannot begin request. Merge request % is not in pending state.', _merge_request_id;
  end if;

  -- select from merge-request the snapshot_sc (s_sc) and plan_rc (p_rc) ids
  select plan_id_receiving_changes, snapshot_id_supplying_changes
    from merlin.merge_request
    where id = _merge_request_id
    into plan_id_receiving, snapshot_id_supplying;

  -- ensure the plan receiving changes isn't locked
  if (select is_locked from merlin.plan where plan.id=plan_id_receiving) then
    raise exception 'Cannot begin merge request. Plan to receive changes is locked.';
  end if;

  -- lock plan_rc
  update merlin.plan
    set is_locked = true
    where plan.id = plan_id_receiving;

  -- get merge base (mb)
  select merlin.get_merge_base(plan_id_receiving, snapshot_id_supplying)
    into merge_base_id;

  -- update the status to "in progress"
  update merlin.merge_request
    set status = 'in-progress',
    merge_base_snapshot_id = merge_base_id,
    reviewer_username = review_username
    where id = _merge_request_id;


  -- perform diff between mb and s_sc (s_diff)
    -- delete is B minus A on key
    -- add is A minus B on key
    -- A intersect B is no op
    -- A minus B on everything except everything currently in the table is modify
  create temp table supplying_diff(
    activity_id integer,
    change_type merlin.activity_change_type not null
  );

  insert into supplying_diff (activity_id, change_type)
  select activity_id, 'delete'
  from(
    select id as activity_id
    from merlin.plan_snapshot_activities
      where snapshot_id = merge_base_id
    except
    select id as activity_id
    from merlin.plan_snapshot_activities
      where snapshot_id = snapshot_id_supplying) a;

  insert into supplying_diff (activity_id, change_type)
  select activity_id, 'add'
  from(
    select id as activity_id
    from merlin.plan_snapshot_activities
      where snapshot_id = snapshot_id_supplying
    except
    select id as activity_id
    from merlin.plan_snapshot_activities
      where snapshot_id = merge_base_id) a;

  insert into supplying_diff (activity_id, change_type)
    select activity_id, 'none'
      from(
        select psa.id as activity_id, name, tags.tag_ids_activity_snapshot(psa.id, merge_base_id),
               source_scheduling_goal_id, created_at, start_offset, type, arguments,
               metadata, anchor_id, anchored_to_start
        from merlin.plan_snapshot_activities psa
        where psa.snapshot_id = merge_base_id
    intersect
      select id as activity_id, name, tags.tag_ids_activity_snapshot(psa.id, snapshot_id_supplying),
             source_scheduling_goal_id, created_at, start_offset, type, arguments,
             metadata, anchor_id, anchored_to_start
        from merlin.plan_snapshot_activities psa
        where psa.snapshot_id = snapshot_id_supplying) a;

  insert into supplying_diff (activity_id, change_type)
    select activity_id, 'modify'
    from(
      select id as activity_id from merlin.plan_snapshot_activities
        where snapshot_id = merge_base_id or snapshot_id = snapshot_id_supplying
      except
      select activity_id from supplying_diff) a;

  -- perform diff between mb and p_rc (r_diff)
  create temp table receiving_diff(
     activity_id integer,
     change_type merlin.activity_change_type not null
  );

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'delete'
  from(
        select id as activity_id
        from merlin.plan_snapshot_activities
        where snapshot_id = merge_base_id
        except
        select id as activity_id
        from merlin.activity_directive
        where plan_id = plan_id_receiving) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'add'
  from(
        select id as activity_id
        from merlin.activity_directive
        where plan_id = plan_id_receiving
        except
        select id as activity_id
        from merlin.plan_snapshot_activities
        where snapshot_id = merge_base_id) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'none'
  from(
        select id as activity_id, name, tags.tag_ids_activity_snapshot(id, merge_base_id),
               source_scheduling_goal_id, created_at, start_offset, type, arguments,
               metadata, anchor_id, anchored_to_start
        from merlin.plan_snapshot_activities psa
        where psa.snapshot_id = merge_base_id
        intersect
        select id as activity_id, name, tags.tag_ids_activity_directive(id, plan_id_receiving),
               source_scheduling_goal_id, created_at, start_offset, type, arguments,
               metadata, anchor_id, anchored_to_start
        from merlin.activity_directive ad
        where ad.plan_id = plan_id_receiving) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'modify'
  from (
        (select id as activity_id
         from merlin.plan_snapshot_activities
         where snapshot_id = merge_base_id
         union
         select id as activity_id
         from merlin.activity_directive
         where plan_id = plan_id_receiving)
        except
        select activity_id
        from receiving_diff) a;


  -- perform diff between s_diff and r_diff
      -- upload the non-conflicts into merge_staging_area
      -- upload conflict into conflicting_activities
  create temp table diff_diff(
    activity_id integer,
    change_type_supplying merlin.activity_change_type not null,
    change_type_receiving merlin.activity_change_type not null
  );

  -- this is going to require us to do the "none" operation again on the remaining modifies
  -- but otherwise we can just dump the 'adds' and 'none' into the merge staging area table

  -- 'delete' against a 'delete' does not enter the merge staging area table
  -- receiving 'delete' against supplying 'none' does not enter the merge staging area table

  insert into merlin.merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, created_by, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
         )
  -- 'adds' can go directly into the merge staging area table
  select _merge_request_id, activity_id, name, tags.tag_ids_activity_snapshot(s_diff.activity_id, psa.snapshot_id),  source_scheduling_goal_id, created_at,
         created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
    from supplying_diff as  s_diff
    join merlin.plan_snapshot_activities psa
      on s_diff.activity_id = psa.id
    where snapshot_id = snapshot_id_supplying and change_type = 'add'
  union
  -- an 'add' between the receiving plan and merge base is actually a 'none'
  select _merge_request_id, activity_id, name, tags.tag_ids_activity_directive(r_diff.activity_id, ad.plan_id),  source_scheduling_goal_id, created_at,
         created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'::merlin.activity_change_type
    from receiving_diff as r_diff
    join merlin.activity_directive ad
      on r_diff.activity_id = ad.id
    where plan_id = plan_id_receiving and change_type = 'add';

  -- put the rest in diff_diff
  insert into diff_diff (activity_id, change_type_supplying, change_type_receiving)
  select activity_id, supplying_diff.change_type as change_type_supplying, receiving_diff.change_type as change_type_receiving
    from receiving_diff
    join supplying_diff using (activity_id)
  where receiving_diff.change_type != 'add' or supplying_diff.change_type != 'add';

  -- ...except for that which is not recorded
  delete from diff_diff
    where (change_type_receiving = 'delete' and  change_type_supplying = 'delete')
       or (change_type_receiving = 'delete' and change_type_supplying = 'none');

  insert into merlin.merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, created_by, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
  )
  -- receiving 'none' and 'modify' against 'none' in the supplying side go into the merge staging area as 'none'
  select _merge_request_id, activity_id, name, tags.tag_ids_activity_directive(diff_diff.activity_id, plan_id),  source_scheduling_goal_id, created_at,
         created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'
    from diff_diff
    join merlin.activity_directive
      on activity_id=id
    where plan_id = plan_id_receiving
      and change_type_supplying = 'none'
      and (change_type_receiving = 'modify' or change_type_receiving = 'none')
  union
  -- supplying 'modify' against receiving 'none' go into the merge staging area as 'modify'
  select _merge_request_id, activity_id, name, tags.tag_ids_activity_snapshot(diff_diff.activity_id, snapshot_id),  source_scheduling_goal_id, created_at,
         created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join merlin.plan_snapshot_activities p
      on diff_diff.activity_id = p.id
    where snapshot_id = snapshot_id_supplying
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'modify')
  union
  -- supplying 'delete' against receiving 'none' go into the merge staging area as 'delete'
    select _merge_request_id, activity_id, name, tags.tag_ids_activity_directive(diff_diff.activity_id, plan_id),  source_scheduling_goal_id, created_at,
         created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join merlin.activity_directive p
      on diff_diff.activity_id = p.id
    where plan_id = plan_id_receiving
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'delete');

  -- 'modify' against a 'modify' must be checked for equality first.
  with false_modify as (
    select activity_id, name, tags.tag_ids_activity_directive(dd.activity_id, psa.snapshot_id) as tags,
           source_scheduling_goal_id, created_at, start_offset, type, arguments, metadata, anchor_id, anchored_to_start
    from merlin.plan_snapshot_activities psa
    join diff_diff dd
      on dd.activity_id = psa.id
    where psa.snapshot_id = snapshot_id_supplying
      and (dd.change_type_receiving = 'modify' and dd.change_type_supplying = 'modify')
    intersect
    select activity_id, name, tags.tag_ids_activity_directive(dd.activity_id, ad.plan_id) as tags,
           source_scheduling_goal_id, created_at, start_offset, type, arguments, metadata, anchor_id, anchored_to_start
    from diff_diff dd
    join merlin.activity_directive ad
      on dd.activity_id = ad.id
    where ad.plan_id = plan_id_receiving
      and (dd.change_type_supplying = 'modify' and dd.change_type_receiving = 'modify'))
  insert into merlin.merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, created_by, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type)
    select _merge_request_id, ad.id, ad.name, tags,  ad.source_scheduling_goal_id, ad.created_at, ad.created_by,
         ad.last_modified_by, ad.start_offset, ad.type, ad.arguments, ad.metadata, ad.anchor_id, ad.anchored_to_start, 'none'
    from false_modify fm
    left join merlin.activity_directive ad
      on (ad.plan_id, ad.id) = (plan_id_receiving, fm.activity_id);

  -- 'modify' against 'delete' and inequal 'modify' against 'modify' goes into conflict table (aka everything left in diff_diff)
  insert into merlin.conflicting_activities (merge_request_id, activity_id, change_type_supplying, change_type_receiving)
  select begin_merge._merge_request_id, activity_id, change_type_supplying, change_type_receiving
  from (select begin_merge._merge_request_id, activity_id
        from diff_diff
        except
        select msa.merge_request_id, activity_id
        from merlin.merge_staging_area msa) a
  join diff_diff using (activity_id);

  -- Fail if there are no differences between the snapshot and the plan getting merged
  validate_non_no_op_status := null;
  select change_type_receiving
  from merlin.conflicting_activities
  where merge_request_id = _merge_request_id
  limit 1
  into validate_non_no_op_status;

  if validate_non_no_op_status is null then
    select change_type
    from merlin.merge_staging_area msa
    where merge_request_id = _merge_request_id
    and msa.change_type != 'none'
    limit 1
    into validate_non_no_op_status;

    if validate_non_no_op_status is null then
      raise exception 'Cannot begin merge. The contents of the two plans are identical.';
    end if;
  end if;


  -- clean up
  drop table supplying_diff;
  drop table receiving_diff;
  drop table diff_diff;
end
$$;

create or replace procedure merlin.commit_merge(_request_id integer)
  language plpgsql as $$
  declare
    validate_noConflicts integer;
    plan_id_R integer;
    snapshot_id_S integer;
begin
  if(select id from merlin.merge_request where id = _request_id) is null then
    raise exception 'Invalid merge request id %.', _request_id;
  end if;

  -- Stop if this merge is not 'in-progress'
  if (select status from merlin.merge_request where id = _request_id) != 'in-progress' then
    raise exception 'Cannot commit a merge request that is not in-progress.';
  end if;

  -- Stop if any conflicts have not been resolved
  select * from merlin.conflicting_activities
  where merge_request_id = _request_id and resolution = 'none'
  limit 1
  into validate_noConflicts;

  if(validate_noConflicts is not null) then
    raise exception 'There are unresolved conflicts in merge request %. Cannot commit merge.', _request_id;
  end if;

  select plan_id_receiving_changes from merlin.merge_request mr where mr.id = _request_id into plan_id_R;
  select snapshot_id_supplying_changes from merlin.merge_request mr where mr.id = _request_id into snapshot_id_S;

  insert into merlin.merge_staging_area(
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at, created_by, last_modified_by,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type)
    -- gather delete data from the opposite tables
    select  _request_id, activity_id, name, tags.tag_ids_activity_directive(ca.activity_id, ad.plan_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'delete'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = _request_id
        and plan_id = plan_id_R
        and ca.change_type_supplying = 'delete'
    union
    select  _request_id, activity_id, name, tags.tag_ids_activity_snapshot(ca.activity_id, psa.snapshot_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'delete'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = _request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_receiving = 'delete'
    union
    select  _request_id, activity_id, name, tags.tag_ids_activity_directive(ca.activity_id, ad.plan_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'none'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.activity_directive ad
        on  ca.activity_id = ad.id
      where ca.resolution = 'receiving'
        and ca.merge_request_id = _request_id
        and plan_id = plan_id_R
        and ca.change_type_receiving = 'modify'
    union
    select  _request_id, activity_id, name, tags.tag_ids_activity_snapshot(ca.activity_id, psa.snapshot_id),
            source_scheduling_goal_id, created_at, created_by, last_modified_by, start_offset, type, arguments, metadata, anchor_id, anchored_to_start,
            'modify'::merlin.activity_change_type
      from  merlin.conflicting_activities ca
      join  merlin.plan_snapshot_activities psa
        on  ca.activity_id = psa.id
      where ca.resolution = 'supplying'
        and ca.merge_request_id = _request_id
        and snapshot_id = snapshot_id_S
        and ca.change_type_supplying = 'modify';

  -- Unlock so that updates can be written
  update merlin.plan
  set is_locked = false
  where id = plan_id_R;

  -- Update the plan's activities to match merge-staging-area's activities
  -- Add
  insert into merlin.activity_directive(
                id, plan_id, name, source_scheduling_goal_id, created_at, created_by, last_modified_by,
                start_offset, type, arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, name, source_scheduling_goal_id, created_at, created_by, last_modified_by,
            start_offset, type, arguments, metadata, anchor_id, anchored_to_start
   from merlin.merge_staging_area
  where merge_staging_area.merge_request_id = _request_id
    and change_type = 'add';

  -- Modify
  insert into merlin.activity_directive(
    id, plan_id, "name", source_scheduling_goal_id, created_at, created_by, last_modified_by,
    start_offset, "type", arguments, metadata, anchor_id, anchored_to_start )
  select  activity_id, plan_id_R, "name", source_scheduling_goal_id, created_at, created_by, last_modified_by,
          start_offset, "type", arguments, metadata, anchor_id, anchored_to_start
  from merlin.merge_staging_area
  where merge_staging_area.merge_request_id = _request_id
    and change_type = 'modify'
  on conflict (id, plan_id)
  do update
  set name = excluded.name,
      source_scheduling_goal_id = excluded.source_scheduling_goal_id,
      created_at = excluded.created_at,
      created_by = excluded.created_by,
      last_modified_by = excluded.last_modified_by,
      start_offset = excluded.start_offset,
      type = excluded.type,
      arguments = excluded.arguments,
      metadata = excluded.metadata,
      anchor_id = excluded.anchor_id,
      anchored_to_start = excluded.anchored_to_start;

  -- Tags
  delete from tags.activity_directive_tags adt
    using merlin.merge_staging_area msa
    where adt.directive_id = msa.activity_id
      and adt.plan_id = plan_id_R
      and msa.merge_request_id = _request_id
      and msa.change_type = 'modify';

  insert into tags.activity_directive_tags(plan_id, directive_id, tag_id)
    select plan_id_R, activity_id, t.id
    from merlin.merge_staging_area msa
    inner join tags.tags t -- Inner join because it's specifically inserting into a tags-association table, so if there are no valid tags we do not want a null value for t.id
    on t.id = any(msa.tags)
    where msa.merge_request_id = _request_id
      and (change_type = 'modify'
       or change_type = 'add')
    on conflict (directive_id, plan_id, tag_id) do nothing;
  -- Presets
  insert into merlin.preset_to_directive(preset_id, activity_id, plan_id)
  select pts.preset_id, pts.activity_id, plan_id_R
  from merlin.merge_staging_area msa
  inner join merlin.preset_to_snapshot_directive pts using (activity_id)
  where pts.snapshot_id = snapshot_id_S
    and msa.merge_request_id = _request_id
    and (msa.change_type = 'add'
     or msa.change_type = 'modify')
  on conflict (activity_id, plan_id)
    do update
    set preset_id = excluded.preset_id;

  -- Delete
  delete from merlin.activity_directive ad
  using merlin.merge_staging_area msa
  where ad.id = msa.activity_id
    and ad.plan_id = plan_id_R
    and msa.merge_request_id = _request_id
    and msa.change_type = 'delete';

  -- Clean up
  delete from merlin.conflicting_activities where merge_request_id = _request_id;
  delete from merlin.merge_staging_area where merge_staging_area.merge_request_id = _request_id;

  update merlin.merge_request
  set status = 'accepted'
  where id = _request_id;

  -- Attach snapshot history
  insert into merlin.plan_latest_snapshot(plan_id, snapshot_id)
  select plan_id_receiving_changes, snapshot_id_supplying_changes
  from merlin.merge_request
  where id = _request_id;
end
$$;

create or replace function merlin.duplicate_plan(_plan_id integer, new_plan_name text, new_owner text)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
  declare
    validate_plan_id integer;
    new_plan_id integer;
    created_snapshot_id integer;
begin
  select id from merlin.plan where plan.id = _plan_id into validate_plan_id;
  if(validate_plan_id is null) then
    raise exception 'Plan % does not exist.', _plan_id;
  end if;

  select merlin.create_snapshot(_plan_id) into created_snapshot_id;

  insert into merlin.plan(revision, name, model_id, duration, start_time, parent_id, owner, updated_by)
    select
        0, new_plan_name, model_id, duration, start_time, _plan_id, new_owner, new_owner
    from merlin.plan where id = _plan_id
    returning id into new_plan_id;
  insert into merlin.activity_directive(
      id, plan_id, name, source_scheduling_goal_id, created_at, created_by, last_modified_at, last_modified_by, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      id, new_plan_id, name, source_scheduling_goal_id, created_at, created_by, last_modified_at, last_modified_by, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from merlin.activity_directive where activity_directive.plan_id = _plan_id;

  with source_plan as (
    select simulation_template_id, arguments, simulation_start_time, simulation_end_time
    from merlin.simulation
    where simulation.plan_id = _plan_id
  )
  update merlin.simulation s
  set simulation_template_id = source_plan.simulation_template_id,
      arguments = source_plan.arguments,
      simulation_start_time = source_plan.simulation_start_time,
      simulation_end_time = source_plan.simulation_end_time
  from source_plan
  where s.plan_id = new_plan_id;

  insert into merlin.preset_to_directive(preset_id, activity_id, plan_id)
    select preset_id, activity_id, new_plan_id
    from merlin.preset_to_directive ptd where ptd.plan_id = _plan_id;

  insert into tags.plan_tags(plan_id, tag_id)
    select new_plan_id, tag_id
    from tags.plan_tags pt where pt.plan_id = _plan_id;
  insert into tags.activity_directive_tags(plan_id, directive_id, tag_id)
    select new_plan_id, directive_id, tag_id
    from tags.activity_directive_tags adt where adt.plan_id = _plan_id;

  insert into merlin.plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end
$$;

create or replace function merlin.get_merge_base(plan_id_receiving_changes integer, snapshot_id_supplying_changes integer)
  returns integer
  language plpgsql as $$
  declare
    result integer;
begin
  select * from
    (
      select merlin.get_snapshot_history_from_plan(plan_id_receiving_changes) as ids
      intersect
      select merlin.get_snapshot_history(snapshot_id_supplying_changes) as ids
    )
    as ids
    order by ids desc
    limit 1
    into result;
  return result;
end
$$;

create or replace function merlin.create_merge_request(plan_id_supplying integer, plan_id_receiving integer, request_username text)
  returns integer
  language plpgsql as $$
declare
  merge_base_snapshot_id integer;
  validate_planIds integer;
  supplying_snapshot_id integer;
  merge_request_id integer;
begin
  if plan_id_receiving = plan_id_supplying then
    raise exception 'Cannot create a merge request between a plan and itself.';
  end if;
  select id from merlin.plan where plan.id = plan_id_receiving into validate_planIds;
  if validate_planIds is null then
    raise exception 'Plan receiving changes (Plan %) does not exist.', plan_id_receiving;
  end if;
  select id from merlin.plan where plan.id = plan_id_supplying into validate_planIds;
  if validate_planIds is null then
    raise exception 'Plan supplying changes (Plan %) does not exist.', plan_id_supplying;
  end if;

  select merlin.create_snapshot(plan_id_supplying) into supplying_snapshot_id;

  select merlin.get_merge_base(plan_id_receiving, supplying_snapshot_id) into merge_base_snapshot_id;
  if merge_base_snapshot_id is null then
    raise exception 'Cannot create merge request between unrelated plans.';
  end if;

  insert into merlin.merge_request(plan_id_receiving_changes, snapshot_id_supplying_changes, merge_base_snapshot_id, requester_username)
    values(plan_id_receiving, supplying_snapshot_id, merge_base_snapshot_id, request_username)
    returning id into merge_request_id;
  return merge_request_id;
end
$$;

create or replace procedure merlin.withdraw_merge_request(request_id integer)
  language plpgsql as
$$
declare
  validate_status merlin.merge_request_status;
begin
  select status from merlin.merge_request where id = request_id into validate_status;
  if validate_status is null then
    raise exception 'Merge request % does not exist. Cannot withdraw request.', request_id;
  elsif validate_status != 'pending' and validate_status != 'withdrawn' then
    raise exception 'Cannot withdraw request.';
  end if;

  update merlin.merge_request
    set status = 'withdrawn'
    where id = request_id;
end
$$;

create or replace procedure merlin.deny_merge(request_id integer)
  language plpgsql as $$
begin
  if(select id from merlin.merge_request where id = request_id) is null then
    raise exception 'Invalid merge request id %.', request_id;
  end if;

  if (select status from merlin.merge_request where id = request_id) != 'in-progress' then
    raise exception 'Cannot reject merge not in progress.';
  end if;

  delete from merlin.conflicting_activities where merge_request_id = request_id;
  delete from merlin.merge_staging_area where merge_staging_area.merge_request_id = deny_merge.request_id;

  update merlin.merge_request
  set status = 'rejected'
  where merge_request.id = request_id;

  update merlin.plan
  set is_locked = false
  where plan.id = (select plan_id_receiving_changes from merlin.merge_request where id = request_id);
end
$$;

create or replace procedure merlin.cancel_merge(request_id integer)
  language plpgsql as $$
declare
  verify_status merlin.merge_request_status;
begin
  if(select id from merlin.merge_request where id = request_id) is null then
    raise exception 'Invalid merge request id %.', request_id;
  end if;

  select status from merlin.merge_request where id = request_id into verify_status;
  if not (verify_status = 'in-progress' or verify_status = 'pending') then
    raise exception 'Cannot cancel merge.';
  end if;

  delete from merlin.conflicting_activities where merge_request_id = request_id;
  delete from merlin.merge_staging_area where merge_staging_area.merge_request_id = cancel_merge.request_id;

  update merlin.merge_request
  set status = 'pending'
  where merge_request.id = request_id;

  update merlin.plan
  set is_locked = false
  where plan.id = (select plan_id_receiving_changes from merlin.merge_request where id = request_id);
end
$$;

create or replace procedure merlin.plan_locked_exception(plan_id integer)
language plpgsql as $$
  begin
    if(select is_locked from merlin.plan p where p.id = plan_id limit 1) then
      raise exception 'Plan % is locked.', plan_id;
    end if;
  end
$$;

comment on procedure merlin.plan_locked_exception(plan_id integer) is e''
  'Verify that the specified plan is unlocked, throwing an exception if not.';

create or replace function merlin.populate_constraint_spec_new_plan()
returns trigger
language plpgsql as $$
begin
  insert into merlin.constraint_specification (plan_id, constraint_id, constraint_revision)
  select new.id, cms.constraint_id, cms.constraint_revision
  from merlin.constraint_model_specification cms
  where cms.model_id = new.model_id;
  return new;
end;
$$;

create or replace function merlin.create_simulation_row_for_new_plan()
returns trigger
security definer
language plpgsql as $$begin
  insert into merlin.simulation (revision, simulation_template_id, plan_id, arguments, simulation_start_time, simulation_end_time)
  values (0, null, new.id, '{}', new.start_time, new.start_time+new.duration);
  return new;
end
$$;

end;
