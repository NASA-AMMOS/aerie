create table parcel (
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
    references channel_dictionary (id),
  foreign key (command_dictionary_id)
    references command_dictionary (id),
  foreign key (sequence_adaptation_id)
    references sequence_adaptation (id)
);

create function parcel_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
before update on parcel
for each row
execute function parcel_set_updated_at();
