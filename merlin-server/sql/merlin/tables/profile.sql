create table profile (
  id integer generated always as identity,
  dataset_id integer not null,

  name text not null,
  type jsonb null,
  duration interval not null,

  constraint profile_synthetic_key
    primary key (id),
  constraint profile_natural_key
    unique (dataset_id, name),
  constraint profile_owned_by_dataset
    foreign key (dataset_id)
    references dataset
    on update cascade
    on delete cascade
);

comment on table profile is e''
  'The behavior of a resource over time, in the context of a dataset.';
comment on column profile.dataset_id is e''
  'The dataset this profile is part of.';
comment on column profile.name is e''
  'A human-readable name for this profile, unique within its containing dataset.';
comment on column profile.type is e''
  'The type of behavior this profile expresses. The segments of this profile must abide by this type.';
comment on column profile.duration is e''
  'The duration of the profile after the start time stored in the dataset.';
