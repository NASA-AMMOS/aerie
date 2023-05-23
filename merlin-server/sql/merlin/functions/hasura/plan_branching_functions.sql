create table hasura_functions.duplicate_plan_return_value(new_plan_id integer);
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

create table hasura_functions.get_plan_history_return_value(plan_id integer);
create function hasura_functions.get_plan_history(plan_id integer)
  returns setof hasura_functions.get_plan_history_return_value
  language plpgsql
  stable as $$
begin
  return query select get_plan_history($1);
end;
$$;
