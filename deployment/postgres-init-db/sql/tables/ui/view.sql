create table view (
  created_at timestamptz not null default now(),
  definition jsonb not null,
  id integer generated always as identity,
  name text not null,
  owner text,
  updated_at timestamptz not null default now(),

  constraint view_primary_key primary key (id)
);

comment on table view is e''
  'View configuration for Aerie UI.';
comment on column view.created_at is e''
  'Time the view was created.';
comment on column view.definition is e''
  'JSON blob of the view definition that implements the view JSON schema.';
comment on column view.id is e''
  'Integer primary key of the view.';
comment on column view.name is e''
  'Human-readable name of the view.';
comment on column view.owner is e''
  'The user who owns the view.';
comment on column view.updated_at is e''
  'Time the view was last updated.';

create or replace function view_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
before update on view
for each row
execute function view_set_updated_at();
