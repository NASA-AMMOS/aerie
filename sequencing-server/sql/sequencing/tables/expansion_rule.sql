create table expansion_rule (
  id integer generated always as identity,
  name text not null,

  activity_type text not null,
  expansion_logic text not null,

  authoring_command_dict_id integer,
  authoring_mission_model_id integer,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  owner integer,
  updated_by integer,

  description text not null default '',

  constraint expansion_rule_unique_name_per_dict_and_model
    unique (authoring_mission_model_id, authoring_command_dict_id, name),

  constraint expansion_rule_primary_key
  primary key (id),

  constraint expansion_rule_activity_type_foreign_key
    unique (id, activity_type),

  foreign key (authoring_command_dict_id)
    references command_dictionary (id)
    on delete set null
);
comment on table expansion_rule is e''
  'The user defined logic to expand an activity type.';
comment on column expansion_rule.id is e''
  'The synthetic identifier for this expansion rule.';
comment on column expansion_rule.activity_type is e''
  'The user selected activity type.';
comment on column expansion_rule.expansion_logic is e''
  'The expansion logic used to generate commands.';
comment on column expansion_rule.authoring_command_dict_id is e''
  'The id of the command dictionary to be used for authoring of this expansion.';
comment on column expansion_rule.authoring_mission_model_id is e''
  'The id of the mission model to be used for authoring of this expansion.';
comment on column expansion_rule.owner is e''
  'The user responsible for this expansion rule.';
comment on column expansion_rule.updated_by is e''
  'The user who last updated this expansion rule.';
comment on column expansion_rule.description is e''
  'A description of this expansion rule.';
comment on column expansion_rule.created_at is e''
  'The time this expansion rule was created';
comment on column expansion_rule.updated_at is e''
  'The time this expansion rule was last updated.';
comment on constraint expansion_rule_activity_type_foreign_key on expansion_rule is e''
  'This enables us to have a foreign key on expansion_set_to_rule which is necessary for building the unique constraint `max_one_expansion_of_each_activity_type_per_expansion_set`.';

create function expansion_rule_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  update expansion_set es
    set updated_at = new.updated_at
    from expansion_set_to_rule esr
    where esr.rule_id = new.id
      and esr.set_id = es.id;
  return new;
end$$;

create trigger set_timestamp
before update on expansion_rule
for each row
execute function expansion_rule_set_updated_at();

create function expansion_rule_default_name()
returns trigger
security invoker
language plpgsql as $$begin
  new.name = new.id;
  return new;
end
$$;

create trigger set_default_name
before insert on expansion_rule
for each row
when ( new.name is null )
execute function expansion_rule_default_name();

