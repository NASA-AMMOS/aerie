-- History of Plans notes (planid to planid):
--  - Grab all non-null parents
create function get_plan_history(starting_plan_id integer)
  returns setof integer
  language plpgsql as $$
  declare
    validate_id integer;
  begin
    select plan.id from plan where plan.id = starting_plan_id into validate_id;
    if validate_id is null then
      raise exception 'Plan ID % is not present in plan table.', starting_plan_id;
    end if;

    return query with recursive history(id) as (
        values(starting_plan_id)                      -- base case
      union
        select parent_id from plan
          join history on history.id = plan.id and plan.parent_id is not null-- recursive case
    ) select * from history;
  end
$$;


-- History of Snapshot notes (planid to snapshotid(s)):
--  - Get the whole history of both
--  - Get the max snapshot id of the intersection
create function get_snapshot_history_from_plan(starting_plan_id integer)
  returns setof integer
  language plpgsql as $$
  begin
    return query
      select get_snapshot_history(snapshot_id)  --runs the recursion
      from plan_latest_snapshot where plan_id = starting_plan_id; --supplies input for get_snapshot_history
  end
$$;

create function get_snapshot_history(starting_snapshot_id integer)
  returns setof integer
  language plpgsql as $$
  declare
    validate_id integer;
begin
  select plan_snapshot.snapshot_id from plan_snapshot where plan_snapshot.snapshot_id = starting_snapshot_id into validate_id;
  if validate_id is null then
    raise exception 'Snapshot ID % is not present in plan_snapshot table.', starting_snapshot_id;
  end if;

  return query with recursive history(id) as (
      values(starting_snapshot_id) --base case
    union
      select parent_snapshot_id from plan_snapshot_parent
        join history on id = plan_snapshot_parent.snapshot_id --recursive case
  ) select * from history;
end
$$;
