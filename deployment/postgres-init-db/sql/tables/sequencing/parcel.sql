create table sequencing.parcel (
  id integer generated always as identity,

  name text not null,

  channel_dictionary_id integer default null,
  command_dictionary_id integer not null,
  sequence_adaptation_id integer default null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  owner text,

  constraint parcel_synthetic_key
    primary key (id),

  foreign key (channel_dictionary_id)
    references sequencing.channel_dictionary (id),
  foreign key (command_dictionary_id)
    references sequencing.command_dictionary (id),
  foreign key (sequence_adaptation_id)
    references sequencing.sequence_adaptation (id)
);

comment on table sequencing.parcel is e''
  'A bundled containing dictionaries and an adaptation file.';
comment on column sequencing.parcel.id is e''
  'The synthetic identifier for this parcel.';
comment on column sequencing.parcel.name is e''
  'The name of the parcel.';
comment on column sequencing.parcel.channel_dictionary_id is e''
  'The identifier for the channel dictionary.';
comment on column sequencing.parcel.command_dictionary_id is e''
  'The identifier for the command dictionary.';
comment on column sequencing.parcel.sequence_adaptation_id is e''
  'The identifier for the adaptation file.';

create trigger set_timestamp
before update on sequencing.parcel
for each row
execute function util_functions.set_updated_at();
