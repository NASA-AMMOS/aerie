create table expansion_set_to_rule (
  set_id integer not null,
  rule_id integer not null,
  activity_type text not null,

  constraint expansion_set_to_rule_primary_key
    primary key (set_id,rule_id),

  foreign key (set_id)
    references expansion_set (id)
    on delete cascade,

  foreign key (rule_id, activity_type)
    references expansion_rule (id, activity_type)
    on delete cascade,

  CONSTRAINT max_one_expansion_of_each_activity_type_per_expansion_set
    UNIQUE (set_id, activity_type)
);
comment on table expansion_set_to_rule is e''
  'The join table between expansion_set and expansion_rule.';
comment on column expansion_set_to_rule.set_id is e''
  'The id for an expansion_set.';
comment on column expansion_set_to_rule.rule_id is e''
  'The id for an expansion_rule.';
comment on column expansion_set_to_rule.activity_type is e''
  'The activity type of the expansion rule. To be used exclusively for the uniqueness check.';
comment on constraint max_one_expansion_of_each_activity_type_per_expansion_set on expansion_set_to_rule is e''
  'Ensures that there is maximum one expansion of each activity type per expansion set.';

create view expansion_set_rule_view as
  select set_id, expansion_rule.*
  from expansion_set_to_rule left join expansion_rule
  on expansion_set_to_rule.rule_id = expansion_rule.id;

create view rule_expansion_set_view as
  select rule_id, expansion_set.*
  from expansion_set_to_rule left join expansion_set
  on expansion_set_to_rule.set_id = expansion_set.id;
