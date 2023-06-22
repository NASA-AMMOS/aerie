create table metadata.scheduling_goal_tags (
  goal_id integer references public.scheduling_goal
    on update cascade
    on delete cascade,
  tag_id integer not null,
  primary key (goal_id, tag_id)
);
comment on table metadata.scheduling_goal_tags is e''
  'The tags associated with a scheduling goal.';
