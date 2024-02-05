create table metadata.scheduling_condition_tags (
  condition_id integer references public.scheduling_condition_metadata
    on update cascade
    on delete cascade,
  tag_id integer not null,
  primary key (condition_id, tag_id)
);
comment on table metadata.scheduling_condition_tags is e''
  'The tags associated with a scheduling condition.';
