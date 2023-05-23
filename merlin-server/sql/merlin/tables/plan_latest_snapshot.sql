create table plan_latest_snapshot(
  plan_id integer,
  snapshot_id integer,

  primary key (plan_id, snapshot_id),
  foreign key (plan_id)
    references plan
    on update cascade
    on delete cascade,
  foreign key (snapshot_id)
    references plan_snapshot
    on update cascade
    on delete cascade
);
