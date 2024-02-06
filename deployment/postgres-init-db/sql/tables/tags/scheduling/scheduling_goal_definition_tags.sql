create table tags.scheduling_goal_definition_tags (
  goal_id integer not null,
  goal_revision integer not null,
  tag_id integer not null references tags.tags
    on update cascade
    on delete cascade,
  primary key (goal_id, goal_revision, tag_id),
  foreign key (goal_id, goal_revision) references scheduler.scheduling_goal_definition
    on update cascade
    on delete cascade
);

comment on table tags.scheduling_goal_definition_tags is e''
  'The tags associated with a specific scheduling condition definition.';
