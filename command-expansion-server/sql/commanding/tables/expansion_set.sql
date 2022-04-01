create table expansion_set (
  id integer generated always as identity,

  command_dict_id integer not null,
  mission_model_id integer not null,

  constraint expansion_set_primary_key
    primary key (id),

  foreign key (command_dict_id)
    references command_dictionary (id)
    on delete cascade
);

comment on table expansion_set is e''
  'A binding of a command dictionary to a mission model.';
comment on column expansion_set.id is e''
  'The synthetic identifier for the set.';
comment on column expansion_set.command_dict_id is e''
  'The ID of a command dictionary.';
comment on column expansion_set.mission_model_id is e''
  'The ID of a mission model.';
