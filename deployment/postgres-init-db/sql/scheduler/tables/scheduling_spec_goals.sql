create table scheduling_spec_goals (
  spec_id integer not null,
  goal_id integer not null,
  priority integer not null,

  constraint scheduling_spec_goals_primary_key
    primary key (spec_id, goal_id),
  constraint scheduling_spec_goals_unique_priorities
    unique (spec_id, priority),
  constraint scheduling_spec_goals_references_scheduling_spec
    foreign key (spec_id)
      references scheduling_spec
      on update cascade
      on delete cascade,
  constraint scheduling_spec_goals_references_scheduling_goals
    foreign key (goal_id)
      references scheduling_goal
      on update cascade
      on delete cascade
);

comment on table scheduling_spec_goals is e''
  'A join table associating scheduling specs with scheduling goals.';
comment on column scheduling_spec_goals.spec_id is e''
  'The ID of the scheduling spec a scheduling goal is associated with.';
comment on column scheduling_spec_goals.goal_id is e''
  'The ID of the scheduling goal a scheduling spec is associated with.';
comment on column scheduling_spec_goals.priority is e''
  'The relative priority of a scheduling goal in relation to other '
  'scheduling goals within the same spec.';
