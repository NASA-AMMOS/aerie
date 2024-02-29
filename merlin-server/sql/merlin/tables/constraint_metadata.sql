create table constraint_metadata(
  id integer generated always as identity,

  name text not null,
  description text not null default '',
  public boolean not null default false,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  owner text,
  updated_by text,

  constraint constraint_metadata_pkey
    primary key (id),
  constraint constraint_owner_exists
    foreign key (owner)
    references metadata.users
    on update cascade
    on delete set null,
  constraint constraint_updated_by_exists
    foreign key (updated_by)
    references metadata.users
    on update cascade
    on delete set null
);

-- A partial index is used to enforce name uniqueness only on constraints visible to other users
create unique index name_unique_if_published on constraint_metadata (name) where public;

comment on table constraint_metadata is e''
  'The metadata for a constraint';
comment on column constraint_metadata.id is e''
  'The unique identifier of the constraint';
comment on column constraint_metadata.name is e''
  'A human-meaningful name.';
comment on column constraint_metadata.description is e''
  'A detailed description suitable for long-form documentation.';
comment on column constraint_metadata.public is e''
  'Whether this constraint is visible to all users.';
comment on column constraint_metadata.owner is e''
  'The user responsible for this constraint.';
comment on column constraint_metadata.updated_by is e''
  'The user who last modified this constraint''s metadata.';
comment on column constraint_metadata.created_at is e''
  'The time at which this constraint was created.';
comment on column constraint_metadata.updated_at is e''
  'The time at which this constraint''s metadata was last modified.';

create function constraint_metadata_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
before update on constraint_metadata
for each row
execute function constraint_metadata_set_updated_at();


