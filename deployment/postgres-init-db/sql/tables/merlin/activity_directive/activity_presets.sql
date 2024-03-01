create table merlin.activity_presets(
  id integer generated always as identity primary key,
  model_id integer not null,
  name text not null,
  associated_activity_type text not null,
  arguments merlin.argument_set not null,
  owner text,

  foreign key (model_id, associated_activity_type)
    references merlin.activity_type
    on delete cascade,
  unique (model_id, associated_activity_type, name),
  constraint activity_presets_owner_exists
    foreign key (owner) references permissions.users
    on update cascade
    on delete set null
);

comment on table merlin.activity_presets is e''
  'A set of arguments that can be applied to an activity of a given type.';

comment on column merlin.activity_presets.id is e''
  'The unique identifier for this activity preset';
comment on column merlin.activity_presets.model_id is e''
  'The model defining this activity preset is associated with.';
comment on column merlin.activity_presets.name is e''
  'The name of this activity preset, unique for an activity type within a mission model.';
comment on column merlin.activity_presets.associated_activity_type is e''
  'The activity type with which this activity preset is associated.';
comment on column merlin.activity_presets.arguments is e''
  'The set of arguments to be applied when this preset is applied.';
comment on column merlin.activity_presets.owner is e''
  'The owner of this activity preset';
