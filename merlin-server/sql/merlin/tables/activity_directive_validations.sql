create table activity_directive_validations (
  directive_id integer not null,
  
  validations jsonb default '{}'::jsonb,

  constraint activity_directive_validations_natural_key
    primary key (directive_id),
  constraint activity_directive_validations_owned_by_activity_directive
    foreign key (directive_id)
    references activity_directive
    on update cascade
    on delete cascade
);

comment on table activity_directive_validations is e''
  'The activity validations extracted from an activity directive.';

comment on column activity_directive_validations.directive_id is e''
  'The activity directive these validations are extracted from.';
comment on column activity_directive_validations.validations is e''
  'The argument validations extracted from an activity directive.';
