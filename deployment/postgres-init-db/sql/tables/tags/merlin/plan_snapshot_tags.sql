create table tags.plan_snapshot_tags(
  snapshot_id integer not null references merlin.plan_snapshot
    on update cascade
    on delete cascade,
  tag_id integer not null references tags.tags
    on update cascade
    on delete cascade,
  primary key (snapshot_id, tag_id)
);

comment on table tags.plan_snapshot_tags is e''
  'The tags associated with a specific. Note: these tags will not be compared in a merge '
  'and will not be applied to the plan if the snapshot is restored.';
