-- Snapshot is a collection of the state of all the activities as they were at the time of the snapshot
-- as well as any other properties of the plan that can change
create table plan_snapshot(
  snapshot_id integer
    generated always as identity
    primary key,

  plan_id integer
    references plan
    on delete set null,

  revision integer not null,
  name text not null,
  duration interval not null,
  start_time timestamptz not null
);
