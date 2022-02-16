create table scheduling_template_goals (
  template_id integer not null,
  goal_id integer not null,
  priority integer not null,

  constraint scheduling_template_goals_primary_key
    primary key (template_id, goal_id),
  constraint scheduling_template_goals_unique_priorities
    unique (template_id, priority),
  constraint scheduling_template_goals_references_scheduling_template
    foreign key (template_id)
      references scheduling_template
      on update cascade
      on delete cascade,
  constraint scheduling_template_goals_references_scheduling_goals
    foreign key (goal_id)
      references scheduling_goal
      on update cascade
      on delete cascade
);

comment on table scheduling_template_goals is e''
  'A join table associating scheduling templates with scheduling goals.';
comment on column scheduling_template_goals.template_id is e''
  'The ID of the scheduling template a scheduling goal is associated with.';
comment on column scheduling_template_goals.goal_id is e''
  'The ID of the scheduling goal a scheduling template is associated with.';
comment on column scheduling_template_goals.priority is e''
  'The relative priority of a scheduling goal in relation to other '
  'scheduling goals within the same template.';
