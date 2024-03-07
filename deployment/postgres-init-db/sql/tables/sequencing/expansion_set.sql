create table sequencing.expansion_set (
  id integer generated always as identity,
  name text not null,
  description text not null default '',

  command_dict_id integer not null,
  mission_model_id integer not null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  owner text,
  updated_by text,

  constraint expansion_set_unique_name_per_dict_and_model
    unique (mission_model_id, command_dict_id, name),

  constraint expansion_set_primary_key
    primary key (id),

  foreign key (command_dict_id)
    references sequencing.command_dictionary (id)
    on delete cascade,
  foreign key (mission_model_id)
    references merlin.mission_model
    on delete cascade,
  foreign key (owner)
    references permissions.users
    on update cascade
    on delete set null,
  foreign key (updated_by)
    references permissions.users
    on update cascade
    on delete set null
);

comment on table sequencing.expansion_set is e''
  'A binding of a command dictionary to a mission model.';
comment on column sequencing.expansion_set.id is e''
  'The synthetic identifier for the set.';
comment on column sequencing.expansion_set.command_dict_id is e''
  'The ID of a command dictionary.';
comment on column sequencing.expansion_set.mission_model_id is e''
  'The ID of a mission model.';
comment on column sequencing.expansion_set.name is e''
  'The human-readable name of the expansion set.';
comment on column sequencing.expansion_set.owner is e''
  'The user responsible for the expansion set.';
comment on column sequencing.expansion_set.updated_by is e''
  'The user who last updated this expansion set.';
comment on column sequencing.expansion_set.description is e''
  'A description of this expansion set.';
comment on column sequencing.expansion_set.created_at is e''
  'The time this expansion set was created';
comment on column sequencing.expansion_set.updated_at is e''
  'The time this expansion set or one of its expansion rules was last updated.';

create function sequencing.expansion_set_default_name()
returns trigger
security invoker
language plpgsql as $$begin
  new.name = new.id;
  return new;
end
$$;

create trigger set_default_name
before insert on sequencing.expansion_set
for each row
when ( new.name is null )
execute function sequencing.expansion_set_default_name();
