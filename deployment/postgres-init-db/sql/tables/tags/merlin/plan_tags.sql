create table tags.plan_tags(
  plan_id integer not null references merlin.plan
    on update cascade
    on delete cascade,
  tag_id integer not null references tags.tags
    on update cascade
    on delete cascade,
  primary key (plan_id, tag_id)
);

comment on table tags.plan_tags is e''
  'The tags associated with a plan. Note: these tags will not be compared during a plan merge.';
