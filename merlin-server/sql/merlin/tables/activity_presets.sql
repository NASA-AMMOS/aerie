create table activity_presets(
  id integer generated always as identity primary key,
  model_id integer not null,
  name text not null,
  associated_activity_type text not null,
  arguments merlin_argument_set not null,

  foreign key (model_id, associated_activity_type)
    references activity_type
    on delete cascade,
  unique (model_id, associated_activity_type, name)
);

comment on table activity_presets is e''
  'A set of arguments that can be applied to an activity of a given type.';

comment on column activity_presets.id is e''
  'The unique identifier for this activity preset';
comment on column activity_presets.model_id is e''
  'The model defining this activity preset is associated with.';
comment on column activity_presets.name is e''
  'The name of this activity preset, unique for an activity type within a mission model.';
comment on column activity_presets.associated_activity_type is e''
  'The activity type with which this activity preset is associated.';
comment on column activity_presets.arguments is e''
  'The set of arguments to be applied when this preset is applied.';

create table preset_to_directive(
  preset_id integer
    references activity_presets
    on update cascade
    on delete cascade,

  activity_id integer,
  plan_id integer,
  foreign key (activity_id, plan_id)
    references activity_directive
    on update cascade
    on delete cascade,

  constraint one_preset_per_activity_directive
    unique (activity_id, plan_id),

  primary key (preset_id, activity_id, plan_id)
);

comment on table preset_to_directive is e''
  'Associates presets with activity directives that have been assigned presets.';

create table preset_to_snapshot_directive(
  preset_id integer
    references activity_presets
    on update cascade
    on delete cascade,

  activity_id integer,
  snapshot_id integer,

  foreign key (activity_id, snapshot_id)
    references plan_snapshot_activities
    on update cascade
    on delete cascade,

  constraint one_preset_per_snapshot_directive
    unique (activity_id, snapshot_id),

  primary key (preset_id, activity_id, snapshot_id)
);

comment on table preset_to_snapshot_directive is e''
  'Associates presets with snapshot activity directives that have been assigned presets.';
