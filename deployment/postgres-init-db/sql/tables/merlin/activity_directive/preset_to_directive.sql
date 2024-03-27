create table merlin.preset_to_directive(
  preset_id integer
    references merlin.activity_presets
    on update cascade
    on delete cascade,

  activity_id integer,
  plan_id integer,
  foreign key (activity_id, plan_id)
    references merlin.activity_directive
    on update cascade
    on delete cascade,

  constraint one_preset_per_activity_directive
    unique (activity_id, plan_id),

  primary key (preset_id, activity_id, plan_id)
);

comment on table merlin.preset_to_directive is e''
  'Associates presets with activity directives that have been assigned presets.';
