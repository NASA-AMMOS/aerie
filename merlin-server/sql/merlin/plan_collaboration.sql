-- TODO list:
--   - plan snapshot table
--   - duplicate function
--   - merge request table
--   - diff function
--   - history tracking (tables and function)
--   - merge base function


-- add parent_id to plan table (this heritage needs to be tracked independently of
alter table plan add column parent_id integer;

-- Plan most recent snapshot
-- Snapshots have history

create table plan_snapshot_parent(
  snapshot_id integer,
  parent_snapshot_id integer
);

create table plan_latest_snapshot(
  plan_id integer,
  snapshot_id integer
);

-- when you take a snapshot of a plan, all of that plan's latest snapshots become the parent snapshots of the new snapshot.

create or replace function duplicate_plan(plan_id integer)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
begin
-- TODO: fill out
end;
$$;
