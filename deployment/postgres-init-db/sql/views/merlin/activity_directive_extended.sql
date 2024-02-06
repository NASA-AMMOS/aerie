create function get_approximate_start_time(_activity_id int, _plan_id int)
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
      from activity_directive ad
      where (ad.id, ad.plan_id) = (_activity_id, _plan_id)
      union
      select ad.id, ad.plan_id, ad.anchor_id, ad.start_offset+gno.net_offset
      from activity_directive ad, get_net_offset gno
      where (ad.id, ad.plan_id) = (gno.anchor_id, gno.plan_id)
    )
    select gno.net_offset, activity_id from get_net_offset gno
    where gno.anchor_id is null
    into _net_offset, _root_activity_id;

  -- Get the plan start time and duration
  select start_time, duration
  from plan
  where id = _plan_id
  into _plan_start_time, _plan_duration;

  select anchored_to_start
  from activity_directive
  where (id, plan_id) = (_root_activity_id, _plan_id)
  into _root_anchored_to_start;

  -- If the root activity is anchored to the end of the plan, add the net to duration
  if not _root_anchored_to_start then
    _net_offset = _plan_duration + _net_offset;
  end if;

  return _plan_start_time+_net_offset;
end
$$;

create view activity_directive_extended as
(
  select
    -- Activity Directive Properties
    ad.id as id,
    ad.plan_id as plan_id,
    -- Additional Properties
    ad.name as name,
    get_tags(ad.id, ad.plan_id) as tags,
    ad.source_scheduling_goal_id as source_scheduling_goal_id,
    ad.created_at as created_at,
    ad.created_by as created_by,
    ad.last_modified_at as last_modified_at,
    ad.last_modified_by as last_modified_by,
    ad.start_offset as start_offset,
    ad.type as type,
    ad.arguments as arguments,
    ad.last_modified_arguments_at as last_modified_arguments_at,
    ad.metadata as metadata,
    ad.anchor_id as anchor_id,
    ad.anchored_to_start as anchored_to_start,
    -- Derived Properties
    get_approximate_start_time(ad.id, ad.plan_id) as approximate_start_time,
    ptd.preset_id as preset_id,
    ap.arguments as preset_arguments
   from activity_directive ad
   left join preset_to_directive ptd on ad.id = ptd.activity_id and ad.plan_id = ptd.plan_id
   left join activity_presets ap on ptd.preset_id = ap.id
);
