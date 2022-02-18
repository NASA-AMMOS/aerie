create table expansion_rules (
  id integer generated always as identity,

  activity_type text not null,
  expansion_logic text not null,

  constraint expansion_rules_primary_key
  primary key (id)
);
comment on table expansion_rules is e''
  'The user defined logic to expand an activity type.';
comment on column expansion_rules.id is e''
  'The synthetic identifier for this expansion rule.';
comment on column expansion_rules.activity_type is e''
  'The user selected activity type.';
comment on column expansion_rules.expansion_logic is e''
  'The expansion logic used to generate commands.';
