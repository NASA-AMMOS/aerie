create table command_dictionary (
  id integer generated always as identity,

  command_types_typescript_path text not null,
  mission text not null,
  version text not null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint command_dictionary_synthetic_key
      primary key (id),
  constraint command_dictionary_natural_key
    unique (mission,version)
);

comment on table command_dictionary is e''
  'A Command Dictionary for a mission.';
comment on column command_dictionary.id is e''
  'The synthetic identifier for this command dictionary.';
comment on column command_dictionary.command_types_typescript_path is e''
  'The location of command dictionary types (.ts) on the filesystem';
comment on column command_dictionary.mission is e''
  'A human-meaningful identifier for the mission described by the command dictionary';
comment on column command_dictionary.version is e''
  'A human-meaningful version qualifier.';
comment on constraint command_dictionary_natural_key on command_dictionary is e''
  'There an only be one command dictionary of a given version for a given mission.';

create function command_dictionary_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger trigger_command_dictionary_set_updated_at
  before
    update on command_dictionary
  for each row
  execute function command_dictionary_set_updated_at();
