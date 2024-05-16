create table sequencing.parcel_to_parameter_dictionary (
  parcel_id integer not null,
  parameter_dictionary_id integer not null,

  foreign key (parcel_id)
    references sequencing.parcel (id)
    on delete cascade,
  foreign key (parameter_dictionary_id)
    references sequencing.parameter_dictionary (id)
    on delete cascade
);

comment on table sequencing.parcel_to_parameter_dictionary is e''
  'Parcels can contain multiple parameter dictionaries so this table keeps track of references between the two.';
