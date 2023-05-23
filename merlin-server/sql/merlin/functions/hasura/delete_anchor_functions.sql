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
