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
