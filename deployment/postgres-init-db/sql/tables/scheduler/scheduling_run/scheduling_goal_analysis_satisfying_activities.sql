create table scheduling_goal_analysis_satisfying_activities (
  analysis_id integer not null,
  goal_id integer not null,
  goal_revision integer not null,
  activity_id integer not null,

  constraint satisfying_activities_primary_key
    primary key (analysis_id, goal_id, goal_revision, activity_id),
  constraint satisfying_activities_references_scheduling_request
    foreign key (analysis_id)
      references scheduling_request (analysis_id)
      on update cascade
      on delete cascade,
  constraint satisfying_activities_references_scheduling_goal
    foreign key (goal_id, goal_revision)
      references scheduling_goal_definition
      on update cascade
      on delete cascade
);

comment on table scheduling_goal_analysis_satisfying_activities is e''
  'The activity instances satisfying a scheduling goal.';
comment on column scheduling_goal_analysis_satisfying_activities.analysis_id is e''
  'The associated analysis ID.';
comment on column scheduling_goal_analysis_satisfying_activities.goal_id is e''
  'The associated goal ID.';
comment on column scheduling_goal_analysis_satisfying_activities.goal_revision is e''
  'The associated version of the goal definition used.';
comment on column scheduling_goal_analysis_satisfying_activities.activity_id is e''
  'The ID of an activity instance satisfying the associated goal.';
