-- UPDATE DUPLICATE PLAN
create or replace function duplicate_plan(_plan_id integer, new_plan_name text, new_owner text)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
  declare
    validate_plan_id integer;
    new_plan_id integer;
    created_snapshot_id integer;
begin
  select id from plan where plan.id = _plan_id into validate_plan_id;
  if(validate_plan_id is null) then
    raise exception 'Plan % does not exist.', _plan_id;
  end if;

  select create_snapshot(_plan_id) into created_snapshot_id;

  insert into plan(revision, name, model_id, duration, start_time, parent_id, owner)
    select
        0, new_plan_name, model_id, duration, start_time, _plan_id, new_owner
    from plan where id = _plan_id
    returning id into new_plan_id;
  insert into activity_directive(
      id, plan_id, name, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      id, new_plan_id, name, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from activity_directive where activity_directive.plan_id = _plan_id;

  with source_plan as (
    select simulation_template_id, arguments, simulation_start_time, simulation_end_time
    from simulation
    where simulation.plan_id = _plan_id
  )
  update simulation s
  set simulation_template_id = source_plan.simulation_template_id,
      arguments = source_plan.arguments,
      simulation_start_time = source_plan.simulation_start_time,
      simulation_end_time = source_plan.simulation_end_time
  from source_plan
  where s.plan_id = new_plan_id;

  insert into preset_to_directive(preset_id, activity_id, plan_id)
    select preset_id, activity_id, new_plan_id
    from preset_to_directive ptd where ptd.plan_id = _plan_id;

  insert into metadata.plan_tags(plan_id, tag_id)
    select new_plan_id, tag_id
    from metadata.plan_tags pt where pt.plan_id = _plan_id;
  insert into metadata.activity_directive_tags(plan_id, directive_id, tag_id)
    select new_plan_id, directive_id, tag_id
    from metadata.activity_directive_tags adt where adt.plan_id = _plan_id;

  insert into plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end
$$;

-- PERMISSIONS IN HASURA FUNCTIONS
drop function hasura_functions.set_resolution_bulk(_merge_request_id integer, _resolution resolution_type, hasura_session json);
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

drop function hasura_functions.set_resolution(_merge_request_id integer, _activity_id integer, _resolution resolution_type, hasura_session json);
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

drop function hasura_functions.cancel_merge(_merge_request_id integer, hasura_session json);
create function hasura_functions.cancel_merge(merge_request_id integer)
  returns hasura_functions.cancel_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call cancel_merge($1);
  return row($1)::hasura_functions.cancel_merge_return_value;
end;
$$;

drop function hasura_functions.withdraw_merge_request(_merge_request_id integer, hasura_session json);
create function hasura_functions.withdraw_merge_request(merge_request_id integer)
  returns hasura_functions.withdraw_merge_request_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call withdraw_merge_request($1);
  return row($1)::hasura_functions.withdraw_merge_request_return_value;
end;
$$;

drop function hasura_functions.deny_merge(merge_request_id integer, hasura_session json);
create function hasura_functions.deny_merge(merge_request_id integer)
  returns hasura_functions.deny_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call deny_merge($1);
  return row($1)::hasura_functions.deny_merge_return_value;
end;
$$;

drop function hasura_functions.commit_merge(_merge_request_id integer, hasura_session json);
create function hasura_functions.commit_merge(merge_request_id integer)
  returns hasura_functions.commit_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call commit_merge($1);
  return row($1)::hasura_functions.commit_merge_return_value;
end;
$$;

drop function hasura_functions.begin_merge(_merge_request_id integer, hasura_session json);
create function hasura_functions.begin_merge(merge_request_id integer, hasura_session json)
  returns hasura_functions.begin_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
  declare
    non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[];
    conflicting_activities hasura_functions.get_conflicting_activities_return_value[];
    reviewer_username text;
begin
  reviewer_username := (hasura_session ->> 'x-hasura-user-id');
  call public.begin_merge($1, reviewer_username);

  non_conflicting_activities := array(select hasura_functions.get_non_conflicting_activities($1));
  conflicting_activities := array(select hasura_functions.get_conflicting_activities($1));

  return row($1, non_conflicting_activities, conflicting_activities)::hasura_functions.begin_merge_return_value;
end;
$$;

drop function hasura_functions.get_conflicting_activities(_merge_request_id integer, hasura_session json);
create function hasura_functions.get_conflicting_activities(merge_request_id integer)
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
    with plan_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, adt.directive_id
      from metadata.tags tags, metadata.activity_directive_tags adt
      where tags.id = adt.tag_id
        and _plan_id_receiving_changes = adt.plan_id
      group by adt.directive_id
    ), snapshot_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, sdt.directive_id, sdt.snapshot_id
      from metadata.tags tags, metadata.snapshot_activity_tags sdt
      where tags.id = sdt.tag_id
        and (sdt.snapshot_id = _snapshot_id_supplying_changes
         or sdt.snapshot_id = _merge_base_snapshot_id)
      group by sdt.directive_id, sdt.snapshot_id
    )
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
      merge_base_act,
      coalesce(st.tags, '[]'),
      coalesce(pt.tags, '[]'),
      coalesce(mbt.tags, '[]')
    from
      (select * from conflicting_activities c where c.merge_request_id = $1) c
        left join plan_snapshot_activities merge_base_act
                  on c.activity_id = merge_base_act.id and _merge_base_snapshot_id = merge_base_act.snapshot_id
        left join plan_snapshot_activities snap_act
                  on c.activity_id = snap_act.id and _snapshot_id_supplying_changes = snap_act.snapshot_id
        left join activity_directive act
                  on _plan_id_receiving_changes = act.plan_id and c.activity_id = act.id
        left join plan_tags pt
                  on c.activity_id = pt.directive_id
        left join snapshot_tags st
                  on c.activity_id = st.directive_id and _snapshot_id_supplying_changes = st.snapshot_id
        left join snapshot_tags mbt
                  on c.activity_id = st.directive_id and _merge_base_snapshot_id = st.snapshot_id;
end;
$$;

drop function hasura_functions.get_non_conflicting_activities(_merge_request_id integer, hasura_session json);
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
    with plan_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, adt.directive_id
      from metadata.tags tags, metadata.activity_directive_tags adt
      where tags.id = adt.tag_id
        and adt.plan_id = _plan_id_receiving_changes
      group by adt.directive_id
    ),
    snapshot_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, sat.directive_id
      from metadata.tags tags, metadata.snapshot_activity_tags sat
      where tags.id = sat.tag_id
        and sat.snapshot_id = _snapshot_id_supplying_changes
      group by sat.directive_id
    )
    select
      activity_id,
      change_type,
      snap_act,
      act,
      coalesce(st.tags, '[]'),
      coalesce(pt.tags, '[]')
    from
      (select msa.activity_id, msa.change_type
       from merge_staging_area msa
       where msa.merge_request_id = $1) c
        left join plan_snapshot_activities snap_act
               on _snapshot_id_supplying_changes = snap_act.snapshot_id
              and c.activity_id = snap_act.id
        left join activity_directive act
               on _plan_id_receiving_changes = act.plan_id
              and c.activity_id = act.id
        left join plan_tags pt
               on c.activity_id = pt.directive_id
        left join snapshot_tags st
               on c.activity_id = st.directive_id;
end
$$;

drop function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, hasura_session json);
create function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, hasura_session json)
  returns hasura_functions.create_merge_request_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
  requester_username text;
begin
  requester_username := (hasura_session ->> 'x-hasura-user-id');
  select create_merge_request(source_plan_id, target_plan_id, requester_username) into res;
  return row(res)::hasura_functions.create_merge_request_return_value;
end;
$$;

drop function hasura_functions.get_plan_history(_plan_id integer, hasura_session json);
create function hasura_functions.get_plan_history(plan_id integer)
  returns setof hasura_functions.get_plan_history_return_value
  language plpgsql
  stable as $$
begin
  return query select get_plan_history($1);
end;
$$;

drop function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json);
create function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json)
  returns hasura_functions.duplicate_plan_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
  new_owner text;
begin
  new_owner := (hasura_session ->> 'x-hasura-user-id');
  select duplicate_plan(plan_id, new_plan_name, new_owner) into res;
  return row(res)::hasura_functions.duplicate_plan_return_value;
end;
$$;

drop function hasura_functions.delete_activity_by_pk_delete_subtree_bulk(_activity_ids int[], _plan_id int, hasura_session json);
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

drop function hasura_functions.delete_activity_by_pk_reanchor_to_anchor_bulk(_activity_ids int[], _plan_id int, hasura_session json);
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

drop function hasura_functions.delete_activity_by_pk_reanchor_plan_start_bulk(_activity_ids int[], _plan_id int, hasura_session json);
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

drop function hasura_functions.delete_activity_by_pk_delete_subtree(_activity_id int, _plan_id int, hasura_session json);
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
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

drop function hasura_functions.delete_activity_by_pk_reanchor_to_anchor(_activity_id int, _plan_id int, hasura_session json);
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
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
end
$$;

drop function hasura_functions.delete_activity_by_pk_reanchor_plan_start(_activity_id int, _plan_id int, hasura_session json);
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
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.last_modified_at, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::activity_directive, 'deleted' from deleted;
  end
$$;

drop function hasura_functions.apply_preset_to_activity(_preset_id int, _activity_id int, _plan_id int, hasura_session json);
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

-- UPDATE VOLATILITY
alter function hasura_functions.get_resources_at_start_offset(_dataset_id integer, _start_offset interval) immutable;

-- NEW FUNCTIONS
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id_receiving integer, _snapshot_id_supplying integer, _user text);
drop procedure metadata.check_merge_permissions(_function metadata.function_permission_key, _merge_request_id integer, hasura_session json);
drop function metadata.raise_if_plan_merge_permission(_function metadata.function_permission_key, _permission metadata.permission);
drop procedure metadata.check_general_permissions(_function metadata.function_permission_key, _permission metadata.permission, _plan_id integer, _user text);
drop function metadata.get_function_permissions(_function metadata.function_permission_key, hasura_session json);
drop function metadata.get_role(hasura_session json);

-- TABLES
drop trigger insert_permissions_when_user_role_created
  on metadata.user_roles;
drop function metadata.insert_permission_for_user_role();

comment on column metadata.user_role_permission.function_permissions is null;
comment on column metadata.user_role_permission.action_permissions is null;
comment on column metadata.user_role_permission.role is null;
comment on table metadata.user_role_permission is null;

drop table metadata.user_role_permission;

-- ENUMS
drop type metadata.function_permission_key;
drop type metadata.action_permission_key;
drop type metadata.permission;

call migrations.mark_migration_rolled_back('21');
