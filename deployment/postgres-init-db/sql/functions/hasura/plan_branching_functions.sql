create table hasura.duplicate_plan_return_value(new_plan_id integer);
create function hasura.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json)
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

create table hasura.get_plan_history_return_value(plan_id integer);
create function hasura.get_plan_history(_plan_id integer, hasura_session json)
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
