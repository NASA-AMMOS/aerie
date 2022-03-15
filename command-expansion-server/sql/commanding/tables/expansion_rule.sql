create table expansion_rule (
  id integer generated always as identity,

  activity_type text not null,
  expansion_logic text not null,

  constraint expansion_rule_primary_key
  primary key (id)
);
comment on table expansion_rule is e''
  'The user defined logic to expand an activity type.';
comment on column expansion_rule.id is e''
  'The synthetic identifier for this expansion rule.';
comment on column expansion_rule.activity_type is e''
  'The user selected activity type.';
comment on column expansion_rule.expansion_logic is e''
  'The expansion logic used to generate commands.';
