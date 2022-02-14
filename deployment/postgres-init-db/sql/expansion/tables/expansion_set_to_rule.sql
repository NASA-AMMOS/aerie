create table expansion_set_to_rule (
  set_id integer not null,
  rule_id integer not null,

  constraint expansion_set_to_rule_primary_key
  primary key (set_id,rule_id),

  foreign key (set_id)
  references expansion_set (id),

  foreign key (rule_id)
  references expansion_rules (id)
);
comment on table expansion_set_to_rule is e''
  'The join table between expansion_set and expansion_rules';
comment on column expansion_set_to_rule.set_id is e''
  'The id for an expansion_set.';
comment on column expansion_set_to_rule.rule_id is e''
  'the id for an expansion_rule.';
