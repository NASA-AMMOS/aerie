create schema hasura_functions;

create table hasura_functions.duplicate_plan_return_value(new_plan_id integer);
create or replace function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text)
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
create or replace function hasura_functions.get_plan_history(plan_id integer)
  returns setof hasura_functions.get_plan_history_return_value
  language plpgsql
  stable as $$
begin
  return query select get_plan_history($1);
end;
$$;

create table hasura_functions.create_merge_request_return_value(merge_request_id integer);
create or replace function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer)
  returns hasura_functions.create_merge_request_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
begin
  select create_merge_request(source_plan_id, target_plan_id) into res;
  return row(res)::hasura_functions.create_merge_request_return_value;
end;
$$;

create table hasura_functions.begin_merge_return_value(merge_request_id integer);
create or replace function hasura_functions.begin_merge(merge_request_id integer)
  returns hasura_functions.begin_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call public.begin_merge($1);
  return row($1)::hasura_functions.begin_merge_return_value;
end;
$$;

create table hasura_functions.commit_merge_return_value(merge_request_id integer);
create or replace function hasura_functions.commit_merge(merge_request_id integer)
  returns hasura_functions.commit_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
declare
  res integer;
begin
  select commit_merge(hasura_functions.commit_merge.merge_request_id) into res;
  return row(res)::hasura_functions.commit_merge_return_value;
end;
$$;

create table hasura_functions.deny_merge_return_value(merge_request_id integer);
create or replace function hasura_functions.deny_merge(merge_request_id integer)
  returns hasura_functions.deny_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
declare
  res integer;
begin
  select deny_merge(hasura_functions.deny_merge.merge_request_id) into res;
  return row(res)::hasura_functions.deny_merge_return_value;
end;
$$;

create table hasura_functions.withdraw_merge_request_return_value(merge_request_id integer);
create or replace function hasura_functions.withdraw_merge_request(merge_request_id integer)
  returns hasura_functions.withdraw_merge_request_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
declare
  res integer;
begin
  select withdraw_merge_request(hasura_functions.withdraw_merge_request.merge_request_id) into res;
  return row(res)::hasura_functions.withdraw_merge_request_return_value;
end;
$$;

create table hasura_functions.cancel_merge_return_value(merge_request_id integer);
create or replace function hasura_functions.cancel_merge(merge_request_id integer)
  returns hasura_functions.cancel_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
begin
  call cancel_merge($1);
  return row($1)::hasura_functions.cancel_merge_return_value;
end;
$$;


create table hasura_functions.get_conflicting_activities_return_value(
    activity_id integer,
    change_type_source activity_change_type,
    change_type_target activity_change_type,
    resolution conflict_resolution,
    source plan_snapshot_activities,
    target activity_directive,
    merge_base plan_snapshot_activities
);
create or replace function hasura_functions.get_conflicting_activities(merge_request_id integer)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  language plpgsql as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
  _merge_base_snapshot_id integer;
begin
  select snapshot_id_supplying_changes, plan_id_receiving_changes, merge_base_snapshot_id
  from merge_request
  where merge_request.id = $1
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes, _merge_base_snapshot_id;

  return query select
         activity_id,
         change_type_supplying,
         change_type_receiving,
         resolution,
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

create table hasura_functions.get_non_conflicting_activities_return_value(
    activity_id integer,
    change_type_source activity_change_type,
    change_type_target activity_change_type,
    resolution conflict_resolution,
    source plan_snapshot_activities,
    target activity_directive
);
create function hasura_functions.get_non_conflicting_activities(merge_request_id integer)
  returns setof hasura_functions.get_non_conflicting_activities_return_value
  strict
  language plpgsql as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
begin
  select snapshot_id_supplying_changes, plan_id_receiving_changes
  from merge_request
  where merge_request.id = $1
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes;

  return query select
         activity_id,
         change_type_supplying,
         change_type_receiving,
         resolution,
         snap_act,
         act
  from
    (select * from conflicting_activities c where c.merge_request_id = $1) c
      left join plan_snapshot_activities snap_act on _snapshot_id_supplying_changes = snap_act.snapshot_id and c.activity_id = snap_act.id
      left join activity_directive act on _plan_id_receiving_changes = act.plan_id and c.activity_id = act.id;
end
$$;
