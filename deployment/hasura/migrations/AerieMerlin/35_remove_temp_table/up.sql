create function get_dependent_activities(_activity_id int, _plan_id int)
  returns table(activity_id int, total_offset interval)
  stable
  language plpgsql as $$
begin
  return query
  with recursive d_activities(activity_id, anchor_id, anchored_to_start, start_offset, total_offset) as (
      select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, ad.start_offset
      from activity_directive ad
      where (ad.anchor_id, ad.plan_id) = (_activity_id, _plan_id) -- select all activities anchored to this one
    union
      select ad.id, ad.anchor_id, ad.anchored_to_start, ad.start_offset, da.total_offset + ad.start_offset
      from activity_directive ad, d_activities da
      where (ad.anchor_id, ad.plan_id) = (da.activity_id, _plan_id) -- select all activities anchored to those in the selection
        and ad.anchored_to_start  -- stop at next end-time anchor
  ) select da.activity_id, da.total_offset
  from d_activities da;
end;
$$;

comment on function get_dependent_activities(_activity_id int, _plan_id int) is e''
'Get the collection of activities that depend on the given activity, with offset relative to the specified activity';

create or replace function validate_anchors()
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
  insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
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
        from activity_directive ad, history h
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
  call validate_nonnegative_net_end_offset(new.id, new.plan_id);
  call validate_nonegative_net_plan_start(new.id, new.plan_id);

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
    from activity_directive ad, end_time_anchor eta
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
    from get_dependent_activities(new.id, new.plan_id)
    where total_offset + offset_from_end_anchor < '0'
    into invalid_descendant_act_ids;

    if invalid_descendant_act_ids is not null then
      raise info 'The following Activity Directives now have a net negative offset relative to an end-time anchor on Activity Directive %: % \n'
        'There may be additional activities that are invalid relative to this activity.',
        end_anchor_id, array_to_string(invalid_descendant_act_ids, ',');

      insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
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
    from activity_directive ad, anchors
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
    from get_dependent_activities(new.id, new.plan_id)
    where total_offset + offset_from_plan_start < '0'
    into invalid_descendant_act_ids;  -- grab all and split

    if invalid_descendant_act_ids is not null then
      raise info 'The following Activity Directives now have a net negative offset relative to Plan Start: % \n'
        'There may be additional activities that are invalid relative to this activity.',
        array_to_string(invalid_descendant_act_ids, ',');

      insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
      select id, new.plan_id, 'Activity Directive ' || id || ' has a net negative offset relative to Plan Start.'
      from unnest(invalid_descendant_act_ids) as id
      on conflict (activity_id, plan_id) do update
        set reason_invalid = 'Activity Directive ' || excluded.activity_id || ' has a net negative offset relative to Plan Start.';
    end if;
  end if;

  -- These are both null iff the activity is anchored to plan end
  if(offset_from_plan_start is null and offset_from_end_anchor is null) then
    -- All dependent activities should have no errors, as Plan End can have an offset of any value.
    insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
    select da.activity_id, new.plan_id, ''
    from get_dependent_activities(new.id, new.plan_id) as da
    on conflict (activity_id, plan_id) do update
      set reason_invalid = '';
  end if;

  -- Remove the error from the dependent activities that wouldn't have been flagged by the earlier checks.
  insert into anchor_validation_status (activity_id, plan_id, reason_invalid)
  select da.activity_id, new.plan_id, ''
  from get_dependent_activities(new.id, new.plan_id) as da
  where total_offset + offset_from_plan_start >= '0'
    or total_offset + offset_from_end_anchor >= '0' -- only one of these checks will run depending on which one has `null` behind the offset
  on conflict (activity_id, plan_id) do update
    set reason_invalid = '';

  return new;
end $$;

call migrations.mark_migration_applied('35');
