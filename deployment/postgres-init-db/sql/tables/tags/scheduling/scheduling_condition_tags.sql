create table tags.scheduling_condition_tags (
  condition_id integer references scheduler.scheduling_condition_metadata
    on update cascade
    on delete cascade,
  tag_id integer not null references tags.tags
    on update cascade
    on delete cascade,
  primary key (condition_id, tag_id)
);
comment on table tags.scheduling_condition_tags is e''
  'The tags associated with a scheduling condition.';
