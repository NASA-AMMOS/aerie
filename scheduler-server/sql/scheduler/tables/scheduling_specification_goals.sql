create table scheduling_specification_goals (
  specification_id integer not null,
  goal_id integer not null,
  priority integer not null,

  constraint scheduling_specification_goals_primary_key
    primary key (specification_id, goal_id),
  constraint scheduling_specification_goals_unique_priorities
    unique (specification_id, priority),
  constraint scheduling_specification_goals_references_scheduling_specification
    foreign key (specification_id)
      references scheduling_specification
      on update cascade
      on delete cascade,
  constraint scheduling_specification_goals_references_scheduling_goals
    foreign key (goal_id)
      references scheduling_goal
      on update cascade
      on delete cascade
);

comment on table scheduling_specification_goals is e''
  'A join table associating scheduling specifications with scheduling goals.';
comment on column scheduling_specification_goals.specification_id is e''
  'The ID of the scheduling specification a scheduling goal is associated with.';
comment on column scheduling_specification_goals.goal_id is e''
  'The ID of the scheduling goal a scheduling specification is associated with.';
comment on column scheduling_specification_goals.priority is e''
  'The relative priority of a scheduling goal in relation to other '
  'scheduling goals within the same specification.';
