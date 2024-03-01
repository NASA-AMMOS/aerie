create table merlin.constraint_definition(
  constraint_id integer not null,
  revision integer not null default 0,
  definition text not null,
  author text,
  created_at timestamptz not null default now(),

  constraint constraint_definition_pkey
    primary key (constraint_id, revision),
  constraint constraint_definition_constraint_exists
    foreign key (constraint_id)
    references merlin.constraint_metadata
    on update cascade
    on delete cascade,
  constraint constraint_definition_author_exists
    foreign key (author)
    references permissions.users
    on update cascade
    on delete set null
);

comment on table merlin.constraint_definition is e''
  'The specific revisions of a constraint''s definition';
comment on column merlin.constraint_definition.revision is e''
  'An identifier of this definition.';
comment on column merlin.constraint_definition.definition is e''
  'An executable expression in the Merlin constraint language.';
comment on column merlin.constraint_definition.author is e''
  'The user who authored this revision.';
comment on column merlin.constraint_definition.created_at is e''
  'When this revision was created.';

create function merlin.constraint_definition_set_revision()
returns trigger
volatile
language plpgsql as $$
declare
  max_revision integer;
begin
  -- Grab the current max value of revision, or -1, if this is the first revision
  select coalesce((select revision
  from merlin.constraint_definition
  where constraint_id = new.constraint_id
  order by revision desc
  limit 1), -1)
  into max_revision;

  new.revision = max_revision + 1;
  return new;
end
$$;

create trigger constraint_definition_set_revision
  before insert on merlin.constraint_definition
  for each row
  execute function merlin.constraint_definition_set_revision();
