create table metadata.activity_directive_tags(
  directive_id integer not null,
  plan_id integer not null,

  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,

  constraint tags_on_existing_activity_directive
    foreign key (directive_id, plan_id)
      references activity_directive
      on update cascade
      on delete cascade,
  primary key (directive_id, plan_id, tag_id)
);

comment on table metadata.activity_directive_tags is e''
  'The tags associated with an activity directive.';
