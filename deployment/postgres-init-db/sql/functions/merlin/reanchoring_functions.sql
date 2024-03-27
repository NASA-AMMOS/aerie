create function merlin.anchor_direct_descendents_to_plan(_activity_id int, _plan_id int)
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
comment on function merlin.anchor_direct_descendents_to_plan(_activity_id integer, _plan_id integer) is e''
'Given the primary key of an activity, reanchor all anchor chains attached to the activity to the plan.\n'
'In the event of an end-time anchor, this function assumes all simulated activities have a duration of 0.';

create function merlin.anchor_direct_descendents_to_ancestor(_activity_id int, _plan_id int)
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
comment on function merlin.anchor_direct_descendents_to_ancestor(_activity_id integer, _plan_id integer) is e''
  'Given the primary key of an activity, reanchor all anchor chains attached to the activity to the anchor of said activity.\n'
  'In the event of an end-time anchor, this function assumes all simulated activities have a duration of 0.';

