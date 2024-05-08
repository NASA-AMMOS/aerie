create table expansion_set (
  id integer generated always as identity,
  name text not null,
  description text not null default '',

  parcel_id integer not null,
  mission_model_id integer not null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  owner text,
  updated_by text,

  constraint expansion_set_unique_name_per_parcel_and_model
    unique (mission_model_id, parcel_id, name),

  constraint expansion_set_primary_key
    primary key (id),

  foreign key (parcel_id)
    references parcel (id)
    on delete cascade
);

comment on table expansion_set is e''
  'A binding of a command dictionary to a mission model.';
comment on column expansion_set.id is e''
  'The synthetic identifier for the set.';
comment on column expansion_set.parcel_id is e''
  'The ID of a parcel.';
comment on column expansion_set.mission_model_id is e''
  'The ID of a mission model.';
comment on column expansion_set.name is e''
  'The human-readable name of the expansion set.';
comment on column expansion_set.owner is e''
  'The user responsible for the expansion set.';
comment on column expansion_set.updated_by is e''
  'The user who last updated this expansion set.';
comment on column expansion_set.description is e''
  'A description of this expansion set.';
comment on column expansion_set.created_at is e''
  'The time this expansion set was created';
comment on column expansion_set.updated_at is e''
  'The time this expansion set or one of its expansion rules was last updated.';

create function expansion_set_default_name()
returns trigger
security invoker
language plpgsql as $$begin
  new.name = new.id;
  return new;
end
$$;

create trigger set_default_name
before insert on expansion_set
for each row
when ( new.name is null )
execute function expansion_set_default_name();
