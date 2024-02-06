create table tags.expansion_rule_tags (
  rule_id integer references sequencing.expansion_rule
    on update cascade
    on delete cascade,
  tag_id integer not null references tags.tags
    on update cascade
    on delete cascade,
  primary key (rule_id, tag_id)
);
comment on table tags.expansion_rule_tags is e''
  'The tags associated with an expansion rule.';
