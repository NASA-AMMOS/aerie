create table metadata.scheduling_condition_definition_tags (
  condition_id integer not null,
  condition_revision integer not null,
  tag_id integer not null,
  primary key (condition_id, condition_revision, tag_id),
  foreign key (condition_id, condition_revision) references scheduling_condition_definition
    on update cascade
    on delete cascade
);

comment on table metadata.scheduling_condition_definition_tags is e''
  'The tags associated with a specific scheduling condition definition.';
