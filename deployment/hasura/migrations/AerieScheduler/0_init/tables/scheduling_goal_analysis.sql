create table scheduling_goal_analysis (
  analysis_id integer not null,
  goal_id integer not null,

  satisfied boolean not null,

  constraint scheduling_goal_analysis_primary_key
    primary key (analysis_id, goal_id),
  constraint scheduling_goal_analysis_references_scheduling_analysis
    foreign key (analysis_id)
      references scheduling_analysis
      on update cascade
      on delete cascade,
  constraint scheduling_goal_analysis_references_scheduling_goal
    foreign key (goal_id)
      references scheduling_goal
      on update cascade
      on delete cascade
);

comment on table scheduling_goal_analysis is e''
  'The analysis of single goal from a scheduling run.';
comment on column scheduling_goal_analysis.analysis_id is e''
  'The associated analysis ID.';
comment on column scheduling_goal_analysis.goal_id is e''
  'The associated goal ID.';
comment on column scheduling_goal_analysis.satisfied is e''
  'Whether the associated goal was satisfied by the scheduling run.';
