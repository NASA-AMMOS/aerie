create table mission_model (
  id integer generated always as identity,
  revision integer not null default 0,

  mission text not null,
  name text not null,
  version text not null,
  description text not null default '',

  owner text,
  jar_id integer not null,

  created_at timestamptz not null default now(),

  constraint mission_model_synthetic_key
    primary key (id),
  constraint mission_model_natural_key
    unique (mission, name, version),
  constraint mission_model_references_jar
    foreign key (jar_id)
    references uploaded_file
    on update cascade
    on delete restrict,
  constraint mission_model_owner_exists
    foreign key (owner) references metadata.users
    on update cascade
    on delete set null
);

comment on table mission_model is e''
  'A Merlin simulation model for a mission.';

comment on column mission_model.id is e''
  'The synthetic identifier for this mission model.';
comment on column mission_model.revision is e''
  'A monotonic clock that ticks for every change to this mission model.';
comment on column mission_model.mission is e''
  'A human-meaningful identifier for the mission described by this model.';
comment on column mission_model.name is e''
  'A human-meaningful model name.';
comment on column mission_model.version is e''
  'A human-meaningful version qualifier.';
comment on column mission_model.owner is e''
  'A human-meaningful identifier for the user responsible for this model.';
comment on column mission_model.jar_id is e''
  'An uploaded JAR file defining the mission model.';
comment on column mission_model.created_at is e''
  'The time this mission model was uploaded into Aerie.';
comment on column mission_model.description is e''
  'A human-meaningful description of the mission model.';


create function increment_revision_on_update_mission_model()
returns trigger
security definer
language plpgsql as $$begin
  update mission_model
  set revision = revision + 1
  where id = new.id;

  return new;
end$$;

create function increment_revision_on_update_mission_model_jar()
returns trigger
security definer
language plpgsql as $$begin
  update mission_model
  set revision = revision + 1
  where jar_id = new.id
    or jar_id = old.id;

  return new;
end$$;

create trigger increment_revision_on_update_mission_model_trigger
after update on mission_model
for each row
when (pg_trigger_depth() < 1)
execute function increment_revision_on_update_mission_model();

create trigger increment_revision_on_update_mission_model_jar_trigger
after update on uploaded_file
for each row
execute function increment_revision_on_update_mission_model_jar();
