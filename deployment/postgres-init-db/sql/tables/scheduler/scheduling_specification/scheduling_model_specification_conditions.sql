create table scheduler.scheduling_model_specification_conditions(
  model_id integer not null,
  condition_id integer not null,
  condition_revision integer, -- latest is NULL

  primary key (model_id, condition_id),
  foreign key (condition_id)
    references scheduler.scheduling_condition_metadata
    on update cascade
    on delete restrict,
  foreign key (condition_id, condition_revision)
    references scheduler.scheduling_condition_definition
    on update cascade
    on delete restrict,
  foreign key (model_id)
    references merlin.mission_model
    on update cascade
    on delete cascade
);

comment on table scheduler.scheduling_model_specification_conditions is e''
'The set of scheduling conditions that all plans using the model should include in their scheduling specification.';
comment on column scheduler.scheduling_model_specification_conditions.model_id is e''
'The model which this specification is for. Half of the primary key.';
comment on column scheduler.scheduling_model_specification_conditions.condition_id is e''
'The id of a specific scheduling condition in the specification. Half of the primary key.';
comment on column scheduler.scheduling_model_specification_conditions.condition_revision is e''
'The version of the scheduling condition definition to use. Leave NULL to use the latest version.';
