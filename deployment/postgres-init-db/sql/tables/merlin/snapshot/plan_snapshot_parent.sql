create table merlin.plan_snapshot_parent(
  snapshot_id integer
    references merlin.plan_snapshot,
  parent_snapshot_id integer
    references merlin.plan_snapshot,

  primary key (snapshot_id, parent_snapshot_id),
  constraint snapshot_cannot_be_own_parent
    check ( snapshot_id != parent_snapshot_id )
);

comment on table merlin.plan_snapshot_parent is e''
  'An association table that tracks the history of snapshots taken on a plan.';
comment on column merlin.plan_snapshot_parent.parent_snapshot_id is e''
  'The snapshot that was considered the latest snapshot for a plan when the id in snapshot_id was taken.'
