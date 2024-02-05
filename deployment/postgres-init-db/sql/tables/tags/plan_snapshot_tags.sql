create table metadata.plan_snapshot_tags(
  snapshot_id integer not null references public.plan_snapshot
      on update cascade
      on delete cascade,
  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,
  primary key (snapshot_id, tag_id)
);

comment on table metadata.plan_snapshot_tags is e''
  'The tags associated with a specific. Note: these tags will not be compared in a merge '
  'and will not be applied to the plan if the snapshot is restored.';
