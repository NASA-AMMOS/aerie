create table metadata.expansion_rule_tags (
  rule_id integer references public.expansion_rule
    on update cascade
    on delete cascade,
  tag_id integer not null,
  primary key (rule_id, tag_id)
);
comment on table metadata.expansion_rule_tags is e''
  'The tags associated with an expansion rule.';
