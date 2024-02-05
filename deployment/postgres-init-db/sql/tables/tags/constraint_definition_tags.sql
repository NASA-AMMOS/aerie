create table metadata.constraint_definition_tags (
  constraint_id integer not null,
  constraint_revision integer not null,
  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,
  primary key (constraint_id, constraint_revision, tag_id),
  foreign key (constraint_id, constraint_revision) references constraint_definition
    on update cascade
    on delete cascade
);

comment on table metadata.constraint_definition_tags is e''
  'The tags associated with a specific constraint defintion.';
