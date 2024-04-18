create table parcel (
  id integer generated always as identity,

  name text not null,

  command_dictionary_id integer not null,
  parameter_dictionary_id integer not null,
  sequence_adaptation_id integer default null,

  created_at timestamptz not null default now(),

  owner text,

  constraint parcel_synthetic_key
    primary key (id),

  foreign key (command_dictionary_id)
    references command_dictionary (id),
  foreign key (parameter_dictionary_id)
    references parameter_dictionary (id),
  foreign key (sequence_adaptation_id)
    references sequence_adaptation (id)
);
