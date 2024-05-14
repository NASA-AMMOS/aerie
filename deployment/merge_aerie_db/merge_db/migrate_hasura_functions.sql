-- Activity Presets
create or replace function hasura.apply_preset_to_activity(_preset_id int, _activity_id int, _plan_id int, hasura_session json)
returns merlin.activity_directive
strict
volatile
language plpgsql as $$
  declare
    returning_directive merlin.activity_directive;
    ad_activity_type text;
    preset_activity_type text;
    _function_permission permissions.permission;
    _user text;
begin
    _function_permission := permissions.get_function_permissions('apply_preset', hasura_session);
    perform permissions.raise_if_plan_merge_permission('apply_preset', _function_permission);
    -- Check valid permissions
    _user := hasura_session ->> 'x-hasura-user-id';
    if not _function_permission = 'NO_CHECK' then
      if _function_permission = 'OWNER' then
        if not exists(select * from merlin.activity_presets ap where ap.id = _preset_id and ap.owner = _user) then
          raise insufficient_privilege
            using message = 'Cannot run ''apply_preset'': '''|| _user ||''' is not OWNER on Activity Preset '
                            || _preset_id ||'.';
        end if;
      end if;
      -- Additionally, the user needs to be OWNER of the plan
      call permissions.check_general_permissions('apply_preset', _function_permission, _plan_id, _user);
    end if;

    if not exists(select id from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity directive % does not exist in plan %', _activity_id, _plan_id;
    end if;
    if not exists(select id from merlin.activity_presets where id = _preset_id) then
      raise exception 'Activity preset % does not exist', _preset_id;
    end if;

    select type from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id) into ad_activity_type;
    select associated_activity_type from merlin.activity_presets where id = _preset_id into preset_activity_type;

    if (ad_activity_type != preset_activity_type) then
      raise exception 'Cannot apply preset for activity type "%" onto an activity of type "%".', preset_activity_type, ad_activity_type;
    end if;

    update merlin.activity_directive
    set arguments = (select arguments from merlin.activity_presets where id = _preset_id)
    where (id, plan_id) = (_activity_id, _plan_id);

    insert into merlin.preset_to_directive(preset_id, activity_id, plan_id)
    select _preset_id, _activity_id, _plan_id
    on conflict (activity_id, plan_id) do update
    set preset_id = _preset_id;

    select * from merlin.activity_directive
    where (id, plan_id) = (_activity_id, _plan_id)
    into returning_directive;

    return returning_directive;
end
$$;

-- Hasura functions for handling anchors during delete
create or replace function hasura.delete_activity_by_pk_reanchor_plan_start(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    _function_permission permissions.permission;
  begin
    _function_permission := permissions.get_function_permissions('delete_activity_reanchor_plan', hasura_session);
    perform permissions.raise_if_plan_merge_permission('delete_activity_reanchor_plan', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call permissions.check_general_permissions('delete_activity_reanchor_plan', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    if not exists(select id from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
    end if;

    return query
      with updated as (
        select merlin.anchor_direct_descendents_to_plan(_activity_id := _activity_id, _plan_id := _plan_id)
      )
      select updated.*, 'updated'
        from updated;

    return query
      with deleted as (
        delete from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id) returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.created_by, deleted.last_modified_at, deleted.last_modified_by, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::merlin.activity_directive, 'deleted' from deleted;
  end
$$;

create or replace function hasura.delete_activity_by_pk_reanchor_to_anchor(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura.delete_anchor_return_value
  strict
  volatile
  language plpgsql as $$
declare
    _function_permission permissions.permission;
begin
    _function_permission := permissions.get_function_permissions('delete_activity_reanchor', hasura_session);
    perform permissions.raise_if_plan_merge_permission('delete_activity_reanchor', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call permissions.check_general_permissions('delete_activity_reanchor', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    if not exists(select id from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
    end if;

    return query
      with updated as (
        select merlin.anchor_direct_descendents_to_ancestor(_activity_id := _activity_id, _plan_id := _plan_id)
      )
      select updated.*, 'updated'
        from updated;
    return query
      with deleted as (
        delete from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id) returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.created_by, deleted.last_modified_at, deleted.last_modified_by, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::merlin.activity_directive, 'deleted' from deleted;
end
$$;

create or replace function hasura.delete_activity_by_pk_delete_subtree(_activity_id int, _plan_id int, hasura_session json)
  returns setof hasura.delete_anchor_return_value
  strict
  volatile
  language plpgsql as $$
declare
  _function_permission permissions.permission;
begin
  _function_permission := permissions.get_function_permissions('delete_activity_subtree', hasura_session);
  perform permissions.raise_if_plan_merge_permission('delete_activity_subtree', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions('delete_activity_subtree', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
  end if;

  if not exists(select id from merlin.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_id, _plan_id;
  end if;

  return query
    with recursive
      descendents(activity_id, p_id) as (
          select _activity_id, _plan_id
          from merlin.activity_directive ad
          where (ad.id, ad.plan_id) = (_activity_id, _plan_id)
        union
          select ad.id, ad.plan_id
          from merlin.activity_directive ad, descendents d
          where (ad.anchor_id, ad.plan_id) = (d.activity_id, d.p_id)
      ),
      deleted as (
          delete from merlin.activity_directive ad
            using descendents
            where (ad.plan_id, ad.id) = (_plan_id, descendents.activity_id)
            returning *
      )
      select (deleted.id, deleted.plan_id, deleted.name, deleted.source_scheduling_goal_id,
              deleted.created_at, deleted.created_by, deleted.last_modified_at, deleted.last_modified_by, deleted.start_offset, deleted.type, deleted.arguments,
              deleted.last_modified_arguments_at, deleted.metadata, deleted.anchor_id, deleted.anchored_to_start)::merlin.activity_directive, 'deleted' from deleted;
end
$$;

-- Bulk versions of Anchor Deletion
create or replace function hasura.delete_activity_by_pk_reanchor_plan_start_bulk(_activity_ids int[], _plan_id int, hasura_session json)
  returns setof hasura.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    activity_id int;
    _function_permission permissions.permission;
  begin
    _function_permission := permissions.get_function_permissions('delete_activity_reanchor_plan_bulk', hasura_session);
    perform permissions.raise_if_plan_merge_permission('delete_activity_reanchor_plan_bulk', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call permissions.check_general_permissions('delete_activity_reanchor_plan_bulk', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    set constraints merlin.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      -- An activity ID might've been deleted in a prior step, so validate that it exists first
      if exists(select id from merlin.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura.delete_activity_by_pk_reanchor_plan_start(activity_id, _plan_id, hasura_session);
      end if;
    end loop;
    set constraints merlin.validate_anchors_update_trigger deferred;
  end
$$;

create or replace function hasura.delete_activity_by_pk_reanchor_to_anchor_bulk(_activity_ids int[], _plan_id int, hasura_session json)
  returns setof hasura.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    activity_id int;
    _function_permission permissions.permission;
  begin
    _function_permission := permissions.get_function_permissions('delete_activity_reanchor_bulk', hasura_session);
    perform permissions.raise_if_plan_merge_permission('delete_activity_reanchor_bulk', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call permissions.check_general_permissions('delete_activity_reanchor_bulk', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    set constraints merlin.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      -- An activity ID might've been deleted in a prior step, so validate that it exists first
      if exists(select id from merlin.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura.delete_activity_by_pk_reanchor_to_anchor(activity_id, _plan_id, hasura_session);
      end if;
    end loop;
    set constraints merlin.validate_anchors_update_trigger deferred;
  end
$$;

create or replace function hasura.delete_activity_by_pk_delete_subtree_bulk(_activity_ids int[], _plan_id int, hasura_session json)
  returns setof hasura.delete_anchor_return_value
  strict
  volatile
language plpgsql as $$
  declare
    activity_id int;
    _function_permission permissions.permission;
  begin
    _function_permission := permissions.get_function_permissions('delete_activity_subtree_bulk', hasura_session);
    perform permissions.raise_if_plan_merge_permission('delete_activity_subtree_bulk', _function_permission);
    if not _function_permission = 'NO_CHECK' then
      call permissions.check_general_permissions('delete_activity_subtree_bulk', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
    end if;

    set constraints merlin.validate_anchors_update_trigger immediate;
    foreach activity_id in array _activity_ids loop
      if exists(select id from merlin.activity_directive where (id, plan_id) = (activity_id, _plan_id)) then
        return query
          select * from hasura.delete_activity_by_pk_delete_subtree(activity_id, _plan_id, hasura_session);
      end if;
    end loop;
    set constraints merlin.validate_anchors_update_trigger deferred;
  end
$$;

create or replace function hasura.get_resources_at_start_offset(_dataset_id int, _start_offset interval)
returns setof hasura.resource_at_start_offset_return_value
strict
stable
security invoker
language plpgsql as $$
begin
  return query
    select distinct on (p.name)
      p.dataset_id, p.id, p.name, p.type, ps.start_offset, ps.dynamics, ps.is_gap
    from merlin.profile p, merlin.profile_segment ps
	  where ps.profile_id = p.id
	    and p.dataset_id = _dataset_id
	    and ps.dataset_id = _dataset_id
	    and ps.start_offset <= _start_offset
	  order by p.name, ps.start_offset desc;
end
$$;

create or replace function hasura.restore_activity_changelog(
  _plan_id integer,
  _activity_directive_id integer,
  _revision integer,
  hasura_session json
)
  returns setof merlin.activity_directive
  volatile
  language plpgsql as $$
declare
  _function_permission permissions.permission;
begin
  _function_permission :=
      permissions.get_function_permissions('restore_activity_changelog', hasura_session);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions(
      'restore_activity_changelog',
      _function_permission, _plan_id,
      (hasura_session ->> 'x-hasura-user-id')
    );
  end if;

  if not exists(select id from merlin.plan where id = _plan_id) then
    raise exception 'Plan % does not exist', _plan_id;
  end if;

  if not exists(select id from merlin.activity_directive where (id, plan_id) = (_activity_directive_id, _plan_id)) then
    raise exception 'Activity Directive % does not exist in Plan %', _activity_directive_id, _plan_id;
  end if;

  if not exists(select revision
                from merlin.activity_directive_changelog
                where (plan_id, activity_directive_id, revision) =
                      (_plan_id, _activity_directive_id, _revision))
  then
    raise exception 'Changelog Revision % does not exist for Plan % and Activity Directive %', _revision, _plan_id, _activity_directive_id;
  end if;

  return query
  update merlin.activity_directive as ad
  set name                       = c.name,
      source_scheduling_goal_id  = c.source_scheduling_goal_id,
      start_offset               = c.start_offset,
      type                       = c.type,
      arguments                  = c.arguments,
      last_modified_arguments_at = c.changed_arguments_at,
      metadata                   = c.metadata,
      anchor_id                  = c.anchor_id,
      anchored_to_start          = c.anchored_to_start,
      last_modified_at           = c.changed_at,
      last_modified_by           = c.changed_by
  from merlin.activity_directive_changelog as c
  where ad.id                    = _activity_directive_id
    and c.activity_directive_id  = _activity_directive_id
    and ad.plan_id               = _plan_id
    and c.plan_id                = _plan_id
    and c.revision               = _revision
  returning ad.*;
end
$$;

create or replace function hasura.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json)
  returns hasura.duplicate_plan_return_value -- plan_id of the new plan
  volatile
  language plpgsql as $$
declare
  res integer;
  new_owner text;
  _function_permission permissions.permission;
begin
  new_owner := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := permissions.get_function_permissions('branch_plan', hasura_session);
  perform permissions.raise_if_plan_merge_permission('branch_plan', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions('branch_plan', _function_permission, plan_id, new_owner);
  end if;

  select merlin.duplicate_plan(plan_id, new_plan_name, new_owner) into res;
  return row(res)::hasura.duplicate_plan_return_value;
end;
$$;

create or replace function hasura.get_plan_history(_plan_id integer, hasura_session json)
  returns setof hasura.get_plan_history_return_value
  stable
  language plpgsql as $$
declare
  _function_permission permissions.permission;
begin
  _function_permission := permissions.get_function_permissions('get_plan_history', hasura_session);
  perform permissions.raise_if_plan_merge_permission('get_plan_history', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions('get_plan_history', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
  end if;

  return query select get_plan_history($1);
end;
$$;

create or replace function hasura.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json)
  returns hasura.duplicate_plan_return_value -- plan_id of the new plan
  volatile
  language plpgsql as $$
declare
  res integer;
  new_owner text;
  _function_permission permissions.permission;
begin
  new_owner := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := permissions.get_function_permissions('branch_plan', hasura_session);
  perform permissions.raise_if_plan_merge_permission('branch_plan', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions('branch_plan', _function_permission, plan_id, new_owner);
  end if;

  select merlin.duplicate_plan(plan_id, new_plan_name, new_owner) into res;
  return row(res)::hasura.duplicate_plan_return_value;
end;
$$;

create or replace function hasura.get_plan_history(_plan_id integer, hasura_session json)
  returns setof hasura.get_plan_history_return_value
  stable
  language plpgsql as $$
declare
  _function_permission permissions.permission;
begin
  _function_permission := permissions.get_function_permissions('get_plan_history', hasura_session);
  perform permissions.raise_if_plan_merge_permission('get_plan_history', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions('get_plan_history', _function_permission, _plan_id, (hasura_session ->> 'x-hasura-user-id'));
  end if;

  return query select get_plan_history($1);
end;
$$;

create or replace function hasura.create_merge_request(source_plan_id integer, target_plan_id integer, hasura_session json)
  returns hasura.create_merge_request_return_value -- plan_id of the new plan
  volatile
  language plpgsql as $$
declare
  res integer;
  requester_username text;
  _function_permission permissions.permission;
begin
  requester_username := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := permissions.get_function_permissions('create_merge_rq', hasura_session);
  call permissions.check_merge_permissions('create_merge_rq', _function_permission, target_plan_id, source_plan_id, requester_username);

  select merlin.create_merge_request(source_plan_id, target_plan_id, requester_username) into res;
  return row(res)::hasura.create_merge_request_return_value;
end;
$$;

create or replace function hasura.get_non_conflicting_activities(_merge_request_id integer, hasura_session json)
  returns setof hasura.get_non_conflicting_activities_return_value
  strict
  volatile
  language plpgsql as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
begin
  call permissions.check_merge_permissions('get_non_conflicting_activities', _merge_request_id, hasura_session);

  select snapshot_id_supplying_changes, plan_id_receiving_changes
  from merlin.merge_request
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
      from tags.tags tags, tags.activity_directive_tags adt
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
      from tags.tags tags, tags.snapshot_activity_tags sat
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
       from merlin.merge_staging_area msa
       where msa.merge_request_id = $1) c
        left join merlin.plan_snapshot_activities snap_act
               on _snapshot_id_supplying_changes = snap_act.snapshot_id
              and c.activity_id = snap_act.id
        left join merlin.activity_directive act
               on _plan_id_receiving_changes = act.plan_id
              and c.activity_id = act.id
        left join plan_tags pt
               on c.activity_id = pt.directive_id
        left join snapshot_tags st
               on c.activity_id = st.directive_id;
end
$$;

create or replace function hasura.get_conflicting_activities(_merge_request_id integer, hasura_session json)
  returns setof hasura.get_conflicting_activities_return_value
  strict
  volatile
  language plpgsql as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
  _merge_base_snapshot_id integer;
begin
  call permissions.check_merge_permissions('get_conflicting_activities', _merge_request_id, hasura_session);

  select snapshot_id_supplying_changes, plan_id_receiving_changes, merge_base_snapshot_id
  from merlin.merge_request
  where merge_request.id = _merge_request_id
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
      from tags.tags tags, tags.activity_directive_tags adt
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
      from tags.tags tags, tags.snapshot_activity_tags sdt
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
        when c.resolution = 'supplying' then 'source'::hasura.resolution_type
        when c.resolution = 'receiving' then 'target'::hasura.resolution_type
        when c.resolution = 'none' then 'none'::hasura.resolution_type
      end,
      snap_act,
      act,
      merge_base_act,
      coalesce(st.tags, '[]'),
      coalesce(pt.tags, '[]'),
      coalesce(mbt.tags, '[]')
    from
      (select * from merlin.conflicting_activities c where c.merge_request_id = _merge_request_id) c
        left join merlin.plan_snapshot_activities merge_base_act
                  on c.activity_id = merge_base_act.id and _merge_base_snapshot_id = merge_base_act.snapshot_id
        left join merlin.plan_snapshot_activities snap_act
                  on c.activity_id = snap_act.id and _snapshot_id_supplying_changes = snap_act.snapshot_id
        left join merlin.activity_directive act
                  on _plan_id_receiving_changes = act.plan_id and c.activity_id = act.id
        left join plan_tags pt
                  on c.activity_id = pt.directive_id
        left join snapshot_tags st
                  on c.activity_id = st.directive_id and _snapshot_id_supplying_changes = st.snapshot_id
        left join snapshot_tags mbt
                  on c.activity_id = st.directive_id and _merge_base_snapshot_id = st.snapshot_id;
end;
$$;

create or replace function hasura.begin_merge(_merge_request_id integer, hasura_session json)
  returns hasura.begin_merge_return_value -- plan_id of the new plan
  strict
  volatile
  language plpgsql as $$
  declare
    non_conflicting_activities hasura.get_non_conflicting_activities_return_value[];
    conflicting_activities hasura.get_conflicting_activities_return_value[];
    reviewer_username text;
begin
  call permissions.check_merge_permissions('begin_merge', _merge_request_id, hasura_session);

  reviewer_username := (hasura_session ->> 'x-hasura-user-id');
  call merlin.begin_merge($1, reviewer_username);

  non_conflicting_activities := array(select hasura.get_non_conflicting_activities($1, hasura_session));
  conflicting_activities := array(select hasura.get_conflicting_activities($1, hasura_session));

  return row($1, non_conflicting_activities, conflicting_activities)::hasura.begin_merge_return_value;
end;
$$;

create or replace function hasura.commit_merge(_merge_request_id integer, hasura_session json)
  returns hasura.commit_merge_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call permissions.check_merge_permissions('commit_merge', _merge_request_id, hasura_session);
  call merlin.commit_merge(_merge_request_id);
  return row(_merge_request_id)::hasura.commit_merge_return_value;
end;
$$;

create or replace function hasura.deny_merge(merge_request_id integer, hasura_session json)
  returns hasura.deny_merge_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call permissions.check_merge_permissions('deny_merge', $1, hasura_session);
  call merlin.deny_merge($1);
  return row($1)::hasura.deny_merge_return_value;
end;
$$;

create or replace function hasura.withdraw_merge_request(_merge_request_id integer, hasura_session json)
  returns hasura.withdraw_merge_request_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call permissions.check_merge_permissions('withdraw_merge_rq', _merge_request_id, hasura_session);
  call merlin.withdraw_merge_request(_merge_request_id);
  return row(_merge_request_id)::hasura.withdraw_merge_request_return_value;
end;
$$;

create or replace function hasura.cancel_merge(_merge_request_id integer, hasura_session json)
  returns hasura.cancel_merge_return_value
  strict
  volatile
  language plpgsql as $$
begin
  call permissions.check_merge_permissions('cancel_merge', _merge_request_id, hasura_session);
  call merlin.cancel_merge(_merge_request_id);
  return row(_merge_request_id)::hasura.cancel_merge_return_value;
end;
$$;

create or replace function hasura.set_resolution(_merge_request_id integer, _activity_id integer, _resolution hasura.resolution_type, hasura_session json)
  returns setof hasura.get_conflicting_activities_return_value
  strict
  volatile
  language plpgsql as $$
  declare
    _conflict_resolution merlin.conflict_resolution;
  begin
    call permissions.check_merge_permissions('set_resolution', _merge_request_id, hasura_session);

    select into _conflict_resolution
      case
        when _resolution = 'source' then 'supplying'::merlin.conflict_resolution
        when _resolution = 'target' then 'receiving'::merlin.conflict_resolution
        when _resolution = 'none' then 'none'::merlin.conflict_resolution
      end;

      update merlin.conflicting_activities ca
      set resolution = _conflict_resolution
      where ca.merge_request_id = _merge_request_id and ca.activity_id = _activity_id;
    return query
    select * from hasura.get_conflicting_activities(_merge_request_id, hasura_session)
      where activity_id = _activity_id
      limit 1;
  end
  $$;

create or replace function hasura.set_resolution_bulk(_merge_request_id integer, _resolution hasura.resolution_type, hasura_session json)
  returns setof hasura.get_conflicting_activities_return_value
  strict
  volatile
  language plpgsql as $$
declare
  _conflict_resolution merlin.conflict_resolution;
begin
  call permissions.check_merge_permissions('set_resolution_bulk', _merge_request_id, hasura_session);

  select into _conflict_resolution
    case
      when _resolution = 'source' then 'supplying'::merlin.conflict_resolution
      when _resolution = 'target' then 'receiving'::merlin.conflict_resolution
      when _resolution = 'none' then 'none'::merlin.conflict_resolution
      end;

  update merlin.conflicting_activities ca
  set resolution = _conflict_resolution
  where ca.merge_request_id = _merge_request_id;
  return query
    select * from hasura.get_conflicting_activities(_merge_request_id, hasura_session);
end
$$;


-- Description must be the last parameter since it has a default value
create or replace function hasura.create_snapshot(_plan_id integer, _snapshot_name text, hasura_session json, _description text default null)
  returns hasura.create_snapshot_return_value
  volatile
  language plpgsql as $$
declare
  _snapshot_id integer;
  _snapshotter text;
  _function_permission permissions.permission;
begin
  _snapshotter := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := permissions.get_function_permissions('create_snapshot', hasura_session);
  perform permissions.raise_if_plan_merge_permission('create_snapshot', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions('create_snapshot', _function_permission, _plan_id, _snapshotter);
  end if;
  if _snapshot_name is null then
    raise exception 'Snapshot name cannot be null.';
  end if;

  select merlin.create_snapshot(_plan_id, _snapshot_name, _description, _snapshotter) into _snapshot_id;
  return row(_snapshot_id)::hasura.create_snapshot_return_value;
end;
$$;

create or replace function hasura.restore_from_snapshot(_plan_id integer, _snapshot_id integer, hasura_session json)
	returns hasura.create_snapshot_return_value
	volatile
	language plpgsql as $$
declare
  _user text;
  _function_permission permissions.permission;
begin
	_user := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := permissions.get_function_permissions('restore_snapshot', hasura_session);
  perform permissions.raise_if_plan_merge_permission('restore_snapshot', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call permissions.check_general_permissions('restore_snapshot', _function_permission, _plan_id, _user);
  end if;

  call merlin.restore_from_snapshot(_plan_id, _snapshot_id);
  return row(_snapshot_id)::hasura.create_snapshot_return_value;
end
$$;
