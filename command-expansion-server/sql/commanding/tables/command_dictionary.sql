create table command_dictionary (
  id integer generated always as identity,

  command_types text not null,
  mission text not null,
  version text not null,

  constraint command_dictionary_primary_key
      primary key (id),
  constraint pair unique (mission,version)
);

comment on table command_dictionary is e''
  'A Command Dictionary for a mission.';
comment on column command_dictionary.id is e''
  'The synthetic identifier for this command dictionary.';
comment on column command_dictionary.command_types is e''
  'The location of command dictionary types (.ts) on the filesystem';
comment on column command_dictionary.mission is e''
  'A human-meaningful identifier for the mission described by the command dictionary';
comment on column command_dictionary.version is e''
  'A human-meaningful version qualifier.';
