create table tags.scheduling_goal_tags (
  goal_id integer references scheduler.scheduling_goal_metadata
    on update cascade
    on delete cascade,
  tag_id integer not null references tags.tags
    on update cascade
    on delete cascade,
  primary key (goal_id, tag_id)
);
comment on table tags.scheduling_goal_tags is e''
  'The tags associated with a scheduling goal.';
