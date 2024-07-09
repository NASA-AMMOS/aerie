create table scheduler.scheduling_goal_analysis_created_activities (
  analysis_id integer not null,
  goal_id integer not null,
  goal_revision integer not null,
  goal_invocation_id integer not null,
  activity_id integer not null,

  constraint created_activities_primary_key
    primary key (analysis_id, goal_id, goal_revision, goal_invocation_id, activity_id),
  constraint created_activities_references_scheduling_request
    foreign key (analysis_id)
      references scheduler.scheduling_request (analysis_id)
      on update cascade
      on delete cascade,
  constraint created_activities_references_scheduling_goal
    foreign key (goal_id, goal_revision)
      references scheduler.scheduling_goal_definition
      on update cascade
      on delete cascade
);

comment on table scheduler.scheduling_goal_analysis_created_activities is e''
  'The activity instances created by a scheduling run to satisfy a goal.';
comment on column scheduler.scheduling_goal_analysis_created_activities.analysis_id is e''
  'The associated analysis ID.';
comment on column scheduler.scheduling_goal_analysis_created_activities.goal_id is e''
  'The associated goal ID.';
comment on column scheduler.scheduling_goal_analysis_created_activities.goal_revision is e''
  'The associated version of the goal definition used.';
comment on column scheduler.scheduling_goal_analysis_created_activities.goal_invocation_id is e''
  'The associated goal invocation ID.';
comment on column scheduler.scheduling_goal_analysis_created_activities.activity_id is e''
  'The ID of an activity instance created to satisfy the associated goal.';
