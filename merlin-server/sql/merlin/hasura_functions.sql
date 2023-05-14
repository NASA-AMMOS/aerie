create schema hasura_functions;

create table hasura_functions.duplicate_plan_return_value(new_plan_id integer);
create function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text)
  returns hasura_functions.duplicate_plan_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
begin
  select duplicate_plan(plan_id, new_plan_name) into res;
  return row(res)::hasura_functions.duplicate_plan_return_value;
end;
$$;

create table hasura_functions.get_plan_history_return_value(plan_id integer);
create function hasura_functions.get_plan_history(plan_id integer)
  returns setof hasura_functions.get_plan_history_return_value
  language plpgsql
  stable as $$
begin
  return query select get_plan_history($1);
end;
$$;

create table hasura_functions.create_merge_request_return_value(merge_request_id integer);
create function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, requester_username text)
  returns hasura_functions.create_merge_request_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
begin
  select create_merge_request(source_plan_id, target_plan_id, requester_username) into res;
  return row(res)::hasura_functions.create_merge_request_return_value;
end;
$$;


create table hasura_functions.get_non_conflicting_activities_return_value(
   activity_id integer,
   change_type activity_change_type,
   source plan_snapshot_activities,
   target activity_directive
);
create function hasura_functions.get_non_conflicting_activities(merge_request_id integer)
  returns setof hasura_functions.get_non_conflicting_activities_return_value
  strict
  language plpgsql stable as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
begin
  select snapshot_id_supplying_changes, plan_id_receiving_changes
  from merge_request
  where merge_request.id = $1
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes;

  return query
    select
      activity_id,
      change_type,
      snap_act,
      act
    from
      (select msa.activity_id, msa.change_type
       from merge_staging_area msa
       where msa.merge_request_id = $1) c
        left join plan_snapshot_activities snap_act
               on _snapshot_id_supplying_changes = snap_act.snapshot_id
              and c.activity_id = snap_act.id
        left join activity_directive act
               on _plan_id_receiving_changes = act.plan_id
              and c.activity_id = act.id;
end
$$;

create type resolution_type as enum ('none','source', 'target');
create table hasura_functions.get_conflicting_activities_return_value(
   activity_id integer,
   change_type_source activity_change_type,
   change_type_target activity_change_type,
   resolution resolution_type,
   source plan_snapshot_activities,
   target activity_directive,
   merge_base plan_snapshot_activities
);
create or replace function hasura_functions.get_conflicting_activities(merge_request_id integer)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  language plpgsql stable as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
  _merge_base_snapshot_id integer;
begin
  select snapshot_id_supplying_changes, plan_id_receiving_changes, merge_base_snapshot_id
  from merge_request
  where merge_request.id = $1
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes, _merge_base_snapshot_id;

  return query
    select
      activity_id,
      change_type_supplying,
      change_type_receiving,
      case
        when c.resolution = 'supplying' then 'source'::resolution_type
        when c.resolution = 'receiving' then 'target'::resolution_type
        when c.resolution = 'none' then 'none'::resolution_type
      end,
      snap_act,
      act,
      merge_base_act
    from
      (select * from conflicting_activities c where c.merge_request_id = $1) c
        left join plan_snapshot_activities merge_base_act
                  on c.activity_id = merge_base_act.id and _merge_base_snapshot_id = merge_base_act.snapshot_id
        left join plan_snapshot_activities snap_act
                  on c.activity_id = snap_act.id and _snapshot_id_supplying_changes = snap_act.snapshot_id
        left join activity_directive act
                  on _plan_id_receiving_changes = act.plan_id and c.activity_id = act.id;
end;
$$;

create table hasura_functions.begin_merge_return_value(
  merge_request_id integer,
  non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[],
  conflicting_activities hasura_functions.get_conflicting_activities_return_value[]
);
create function hasura_functions.begin_merge(merge_request_id integer, reviewer_username text)
  returns hasura_functions.begin_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
  declare
    non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[];
    conflicting_activities hasura_functions.get_conflicting_activities_return_value[];
begin
  call public.begin_merge($1, $2);

  non_conflicting_activities := array(select hasura_functions.get_non_conflicting_activities($1));
  conflicting_activities := array(select hasura_functions.get_conflicting_activities($1));

  return row($1, non_conflicting_activities, conflicting_activities)::hasura_functions.begin_merge_return_value;
end;
$$;

create table hasura_functions.commit_merge_return_value(merge_request_id integer);
create function hasura_functions.commit_merge(merge_request_id integer)
  returns hasura_functions.commit_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call commit_merge($1);
  return row($1)::hasura_functions.commit_merge_return_value;
end;
$$;

create table hasura_functions.deny_merge_return_value(merge_request_id integer);
create function hasura_functions.deny_merge(merge_request_id integer)
  returns hasura_functions.deny_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call deny_merge($1);
  return row($1)::hasura_functions.deny_merge_return_value;
end;
$$;

create table hasura_functions.withdraw_merge_request_return_value(merge_request_id integer);
create function hasura_functions.withdraw_merge_request(merge_request_id integer)
  returns hasura_functions.withdraw_merge_request_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call withdraw_merge_request($1);
  return row($1)::hasura_functions.withdraw_merge_request_return_value;
end;
$$;

create table hasura_functions.cancel_merge_return_value(merge_request_id integer);
create function hasura_functions.cancel_merge(merge_request_id integer)
  returns hasura_functions.cancel_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call cancel_merge($1);
  return row($1)::hasura_functions.cancel_merge_return_value;
end;
$$;

create function hasura_functions.set_resolution(_merge_request_id integer, _activity_id integer, _resolution resolution_type)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  language plpgsql as $$
  declare
    _conflict_resolution conflict_resolution;
  begin
    select into _conflict_resolution
      case
        when _resolution = 'source' then 'supplying'::conflict_resolution
        when _resolution = 'target' then 'receiving'::conflict_resolution
        when _resolution = 'none' then 'none'::conflict_resolution
      end;

      update conflicting_activities ca
      set resolution = _conflict_resolution
      where ca.merge_request_id = _merge_request_id and ca.activity_id = _activity_id;
    return query
    select * from hasura_functions.get_conflicting_activities(_merge_request_id)
      where activity_id = _activity_id
      limit 1;
  end
  $$;
create function hasura_functions.set_resolution_bulk(_merge_request_id integer, _resolution resolution_type)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  language plpgsql as $$
declare
  _conflict_resolution conflict_resolution;
begin
  select into _conflict_resolution
    case
      when _resolution = 'source' then 'supplying'::conflict_resolution
      when _resolution = 'target' then 'receiving'::conflict_resolution
      when _resolution = 'none' then 'none'::conflict_resolution
      end;

  update conflicting_activities ca
  set resolution = _conflict_resolution
  where ca.merge_request_id = _merge_request_id;
  return query
    select * from hasura_functions.get_conflicting_activities(_merge_request_id);
end
$$;

-- Hasura functions for handling anchors during delete
create table hasura_functions.delete_anchor_return_value(
  affected_row activity_directive,
  change_type text
);
create function hasura_functions.delete_activity_by_pk_reanchor_plan_start(_activity_id int, _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
language plpgsql as $$
  begin
    if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
    end if;

    return query
      with updated as (
        select public.anchor_direct_descendents_to_plan(_activity_id := _activity_id, _plan_id := _plan_id)
      )
      select updated.*, 'updated'
        from updated;

    return query
      with deleted as (
        delete from activity_directive where (id, plan_id) = (_activity_id, _plan_id) returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.tags, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
  end
$$;

create function hasura_functions.delete_activity_by_pk_reanchor_to_anchor(_activity_id int, _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  language plpgsql as $$
begin
  if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

    return query
      with updated as (
        select public.anchor_direct_descendents_to_ancestor(_activity_id := _activity_id, _plan_id := _plan_id)
      )
      select updated.*, 'updated'
        from updated;
    return query
      with deleted as (
        delete from activity_directive where (id, plan_id) = (_activity_id, _plan_id) returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.tags, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

create function hasura_functions.delete_activity_by_pk_delete_subtree(_activity_id int, _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
  language plpgsql as $$
begin
  if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

  return query
    with recursive
      descendents(activity_id, p_id) as (
          select _activity_id, _plan_id
          from activity_directive ad
          where (ad.id, ad.plan_id) = (_activity_id, _plan_id)
        union
          select ad.id, ad.plan_id
          from activity_directive ad, descendents d
          where (ad.anchor_id, ad.plan_id) = (d.activity_id, d.p_id)
      ),
      deleted as (
          delete from activity_directive ad
            using descendents
            where (ad.plan_id, ad.id) = (_plan_id, descendents.activity_id)
            returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.tags, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

-- Bulk versions of Anchor Deletion
create function hasura_functions.delete_activity_by_pk_reanchor_plan_start_bulk(_activity_ids int[], _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
language plpgsql as $$
  declare activity_id int;
  begin
    set constraints public.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      -- An activity ID might've been deleted in a prior step, so validate that it exists first
      if exists(select id from public.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura_functions.delete_activity_by_pk_reanchor_plan_start(activity_id, _plan_id);
      end if;
    end loop;
    set constraints public.validate_anchors_update_trigger deferred;
  end
$$;

create function hasura_functions.delete_activity_by_pk_reanchor_to_anchor_bulk(_activity_ids int[], _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
language plpgsql as $$
  declare activity_id int;
  begin
    set constraints public.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      -- An activity ID might've been deleted in a prior step, so validate that it exists first
      if exists(select id from public.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura_functions.delete_activity_by_pk_reanchor_to_anchor(activity_id, _plan_id);
      end if;
    end loop;
    set constraints public.validate_anchors_update_trigger deferred;
  end
$$;

create function hasura_functions.delete_activity_by_pk_delete_subtree_bulk(_activity_ids int[], _plan_id int)
  returns setof hasura_functions.delete_anchor_return_value
  strict
language plpgsql as $$
  declare activity_id int;
  begin
    set constraints public.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      if exists(select id from public.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura_functions.delete_activity_by_pk_delete_subtree(activity_id, _plan_id);
      end if;
    end loop;
    set constraints public.validate_anchors_update_trigger deferred;
  end
$$;

-- Activity Presets
create function hasura_functions.apply_preset_to_activity(_preset_id int, _activity_id int, _plan_id int)
returns activity_directive
strict
language plpgsql as $$
  declare
    returning_directive activity_directive;
    ad_activity_type text;
    preset_activity_type text;
begin
    if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity directive % does not exist in plan %', _activity_id, _plan_id;
    end if;
    if not exists(select id from public.activity_presets where id = _preset_id) then
      raise exception 'Activity preset % does not exist', _preset_id;
    end if;

    select type from activity_directive where (id, plan_id) = (_activity_id, _plan_id) into ad_activity_type;
    select associated_activity_type from activity_presets where id = _preset_id into preset_activity_type;

    if (ad_activity_type != preset_activity_type) then
      raise exception 'Cannot apply preset for activity type "%" onto an activity of type "%".', preset_activity_type, ad_activity_type;
    end if;

    update activity_directive
    set arguments = (select arguments from activity_presets where id = _preset_id)
    where (id, plan_id) = (_activity_id, _plan_id);

    insert into preset_to_directive(preset_id, activity_id, plan_id)
    select _preset_id, _activity_id, _plan_id
    on conflict (activity_id, plan_id) do update
    set preset_id = _preset_id;

    select * from activity_directive
    where (id, plan_id) = (_activity_id, _plan_id)
    into returning_directive;

    return returning_directive;
end
$$;

-- Simulation Resources
create table hasura_functions.resource_at_start_offset_return_value(
  dataset_id integer not null,
  id integer not null,
  name text not null,
  type jsonb,
  start_offset interval not null,
  dynamics jsonb,
  is_gap bool not null
);

create function hasura_functions.get_resources_at_start_offset(_dataset_id int, _start_offset interval)
returns setof hasura_functions.resource_at_start_offset_return_value
strict
immutable
security invoker
language plpgsql as $$
begin
  return query
    select distinct on (p.name)
      p.dataset_id, p.id, p.name, p.type, ps.start_offset, ps.dynamics, ps.is_gap
    from profile p, profile_segment ps
	  where ps.profile_id = p.id
	    and p.dataset_id = _dataset_id
	    and ps.dataset_id = _dataset_id
	    and ps.start_offset <= _start_offset
	  order by p.name, ps.start_offset desc;
end
$$;
