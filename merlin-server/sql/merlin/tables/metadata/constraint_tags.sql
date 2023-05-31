create table metadata.constraint_tags (
  constraint_id integer not null references public."constraint"
    on update cascade
    on delete cascade,
  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,
  primary key (constraint_id, tag_id)
);

comment on table metadata.constraint_tags is e''
  'The tags associated with a constraint.';
