create table tags.constraint_tags (
  constraint_id integer not null references merlin.constraint_metadata
    on update cascade
    on delete cascade,
  tag_id integer not null references tags.tags
    on update cascade
    on delete cascade,
  primary key (constraint_id, tag_id)
);

comment on table tags.constraint_tags is e''
  'The tags associated with a constraint.';
