create table plan_snapshot_parent(
  snapshot_id integer
    references plan_snapshot,
  parent_snapshot_id integer
    references plan_snapshot,

  primary key (snapshot_id, parent_snapshot_id),
  constraint snapshot_cannot_be_own_parent
    check ( snapshot_id != parent_snapshot_id )
);
