create table scheduling_specification_conditions (
  specification_id integer not null,
  condition_id integer not null,
  enabled boolean default true,

  constraint scheduling_specification_conditions_primary_key
    primary key (specification_id, condition_id),
  constraint scheduling_specification_conditions_references_scheduling_specification
    foreign key (specification_id)
      references scheduling_specification
      on update cascade
      on delete cascade,
  constraint scheduling_specification_conditions_references_scheduling_conditions
    foreign key (condition_id)
      references scheduling_condition
      on update cascade
      on delete cascade
);

comment on table scheduling_specification_conditions is e''
  'A join table associating scheduling specifications with scheduling conditions.';
comment on column scheduling_specification_conditions.specification_id is e''
  'The ID of the scheduling specification a scheduling goal is associated with.';
comment on column scheduling_specification_conditions.condition_id is e''
  'The ID of the condition a scheduling specification is associated with.';
