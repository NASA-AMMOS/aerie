create table metadata.snapshot_activity_tags(
  directive_id integer not null,
  snapshot_id integer not null,

  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,

  constraint tags_on_existing_snapshot_directive
    foreign key (directive_id, snapshot_id)
      references plan_snapshot_activities
      on update cascade
      on delete cascade,
  primary key (directive_id, snapshot_id, tag_id)
);

comment on table metadata.snapshot_activity_tags is e''
  'The tags associated with an activity directive snapshot.';



