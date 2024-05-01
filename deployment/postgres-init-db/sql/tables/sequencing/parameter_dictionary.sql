create table parameter_dictionary (
  id integer generated always as identity,

  mission text not null,
  version text not null,
  parsed_json jsonb not null default '{}',

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint parameter_dictionary_synthetic_key
      primary key (id),
  constraint parameter_dictionary_natural_key
    unique (mission,version)
);

comment on table parameter_dictionary is e''
  'A Parameter Dictionary for a mission.';
comment on column parameter_dictionary.id is e''
  'The synthetic identifier for this parameter dictionary.';
comment on column parameter_dictionary.mission is e''
  'A human-meaningful identifier for the mission described by the parameter dictionary';
comment on column parameter_dictionary.version is e''
  'A human-meaningful version qualifier.';
comment on column parameter_dictionary.parsed_json is e''
  'The XML that has been parsed and converted to JSON';
comment on constraint parameter_dictionary_natural_key on parameter_dictionary is e''
  'There can only be one dictionary of a given version for a given mission.';

create function parameter_dictionary_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
before update on parameter_dictionary
for each row
execute function parameter_dictionary_set_updated_at();
