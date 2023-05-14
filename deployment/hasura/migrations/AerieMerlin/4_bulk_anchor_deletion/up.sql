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

call migrations.mark_migration_applied('4');
