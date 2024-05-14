create table merlin.preset_to_snapshot_directive(
  preset_id integer
    references merlin.activity_presets
    on update cascade
    on delete cascade,

  activity_id integer,
  snapshot_id integer,

  foreign key (activity_id, snapshot_id)
    references merlin.plan_snapshot_activities
    on update cascade
    on delete cascade,

  constraint one_preset_per_snapshot_directive
    unique (activity_id, snapshot_id),

  primary key (preset_id, activity_id, snapshot_id)
);

comment on table merlin.preset_to_snapshot_directive is e''
  'Associates presets with snapshot activity directives that have been assigned presets.';
