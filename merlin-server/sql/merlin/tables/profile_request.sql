create table profile_request (
  dataset_id integer not null,
  profile_name text not null,
  duration interval not null,

  constraint profile_request_synthetic_key
    primary key (dataset_id, profile_name),
  constraint simulation_determines_dataset
    foreign key (dataset_id)
    references dataset
    on update cascade
    on delete set null
);


comment on table profile_request is e''
  'An expressed desire for information about a profile in a dataset. Upstream systems may prioritize'
  'dataset generation by downstream desire.';

comment on column profile_request.dataset_id is e''
  'The dataset about which information is requested.';
comment on column profile_request.profile_name is e''
  'The profile about which information is requested.';
comment on column profile_request.duration is e''
  'The amount of information requested from the dataset start time up to the given duration.';
