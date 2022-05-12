create table expansion_rule (
  id integer generated always as identity,

  activity_type text not null,
  expansion_logic text not null,

  authoring_command_dict_id integer,
  authoring_mission_model_id integer,

  constraint expansion_rule_primary_key
  primary key (id),

  constraint expansion_rule_activity_type_foreign_key
    unique (id, activity_type),

  foreign key (authoring_command_dict_id)
    references command_dictionary (id)
    on delete cascade
);
comment on table expansion_rule is e''
  'The user defined logic to expand an activity type.';
comment on column expansion_rule.id is e''
  'The synthetic identifier for this expansion rule.';
comment on column expansion_rule.activity_type is e''
  'The user selected activity type.';
comment on column expansion_rule.expansion_logic is e''
  'The expansion logic used to generate commands.';
comment on column expansion_rule.authoring_command_dict_id is e''
  'The id of the command dictionary to be used for authoring of this expansion.';
comment on column expansion_rule.authoring_mission_model_id is e''
  'The id of the mission model to be used for authoring of this expansion.';
comment on constraint expansion_rule_activity_type_foreign_key on expansion_rule is e''
  'This enables us to have a foreign key on expansion_set_to_rule which is necessary for building the unique constraint `max_one_expansion_of_each_activity_type_per_expansion_set`.';
