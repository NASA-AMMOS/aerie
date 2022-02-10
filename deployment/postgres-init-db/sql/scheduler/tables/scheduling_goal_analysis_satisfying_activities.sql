create table scheduling_goal_analysis_satisfying_activities (
  analysis_id integer not null,
  goal_id integer not null,
  activity_id integer not null,

  constraint satisfying_activities_primary_key
    primary key (analysis_id, goal_id, activity_id),
  constraint satisfying_activities_references_scheduling_analysis
    foreign key (analysis_id)
      references scheduling_analysis
      on update cascade
      on delete cascade,
  constraint satisfying_activities_references_scheduling_goal
    foreign key (goal_id)
      references scheduling_goal
      on update cascade
      on delete cascade
);

comment on table scheduling_goal_analysis_satisfying_activities is e''
  'The activity instances satisfying a scheduling goal.';
comment on column scheduling_goal_analysis_satisfying_activities.analysis_id is e''
  'The associated analysis ID.';
comment on column scheduling_goal_analysis_satisfying_activities.goal_id is e''
  'The associated goal ID.';
comment on column scheduling_goal_analysis_satisfying_activities.activity_id is e''
  'The ID of an activity instance satisfying the associated goal.';
