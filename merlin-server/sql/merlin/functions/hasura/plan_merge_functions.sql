create table hasura_functions.create_merge_request_return_value(merge_request_id integer);
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


create table hasura_functions.get_non_conflicting_activities_return_value(
   activity_id integer,
   change_type activity_change_type,
   source plan_snapshot_activities,
   target activity_directive,
   source_tags jsonb,
   target_tags jsonb
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

create type resolution_type as enum ('none','source', 'target');
create table hasura_functions.get_conflicting_activities_return_value(
   activity_id integer,
   change_type_source activity_change_type,
   change_type_target activity_change_type,
   resolution resolution_type,
   source plan_snapshot_activities,
   target activity_directive,
   merge_base plan_snapshot_activities,
   source_tags jsonb,
   target_tags jsonb,
   merge_base_tags jsonb
);
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

create table hasura_functions.begin_merge_return_value(
  merge_request_id integer,
  non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[],
  conflicting_activities hasura_functions.get_conflicting_activities_return_value[]
);
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
