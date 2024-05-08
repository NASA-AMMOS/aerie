create table parcel_to_parameter_dictionary (
  id integer generated always as identity,

  parcel_id integer not null,
  parameter_dictionary_id integer not null,

  constraint parcel_to_parameter_dictionary_synthetic_key
    primary key (id),

  foreign key (parcel_id)
    references parcel (id)
    on delete cascade,
  foreign key (parameter_dictionary_id)
    references parameter_dictionary (id)
    on delete cascade
);

comment on table public.parcel_to_parameter_dictionary is e''
  'Parcels can contain multiple parameter dictionaries so this table keeps track of references between the two.';