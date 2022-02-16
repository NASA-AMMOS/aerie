create table mission_model_parameters (
  model_id integer not null,
  revision integer not null default 0,

  parameters merlin_parameter_set not null,

  constraint mission_model_parameter_natural_key
    primary key (model_id),
  constraint mission_model_parameter_owned_by_mission_model
    foreign key (model_id)
    references mission_model
    on update cascade
    on delete cascade
);

comment on table mission_model_parameters is e''
  'The model parameters extracted from a mission model.';

comment on column mission_model_parameters.model_id is e''
  'The model these parameters are extracted from.';
comment on column mission_model_parameters.revision is e''
  'The revision of the model these parameters are extracted from.';
comment on column mission_model_parameters.parameters is e''
  'The Merlin parameter definitions extracted from a mission model.';
