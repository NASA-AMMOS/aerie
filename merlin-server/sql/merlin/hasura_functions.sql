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
  returns setof hasura_functions.get_plan_history_return_value -- plan_id of the new plan
  language plpgsql
  stable as $$
begin
  return query select get_plan_history($1);
end;
$$;
