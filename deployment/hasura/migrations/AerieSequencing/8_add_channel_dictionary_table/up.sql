----- NOTE: Test file, should not be used in real database migration

--- Database Change

--- create parameter_dictionary_table
create table channel_dictionary (
                                  id integer generated always as identity,

                                  mission text not null,
                                  version text not null,
                                  parsed_json jsonb not null default '{}',

                                  created_at timestamptz not null default now(),

                                  constraint channel_dictionary_synthetic_key
                                    primary key (id),
                                  constraint channel_dictionary_natural_key
                                    unique (mission,version)
);

comment on table channel_dictionary is e''
  'A Channel Dictionary for a mission.';
comment on column channel_dictionary.id is e''
  'The synthetic identifier for this channel dictionary.';
comment on column channel_dictionary.mission is e''
  'A human-meaningful identifier for the mission described by the channel dictionary';
comment on column channel_dictionary.version is e''
  'A human-meaningful version qualifier.';
comment on column channel_dictionary.parsed_json is e''
  'The XML that has been parsed and converted to JSON';
comment on constraint channel_dictionary_natural_key on channel_dictionary is e''
  'There can only be one channel dictionary of a given version for a given mission.';

--- Data migration logic

--- Database Check
select * from "public"."channel_dictionary";
