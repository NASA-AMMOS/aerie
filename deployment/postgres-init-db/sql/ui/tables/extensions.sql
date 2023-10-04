create table extensions (
  description text,
  id integer generated always as identity,
  label text not null,
  owner text,
  url text not null,
  updated_at timestamptz not null default now(),

  constraint extension_primary_key primary key (id)
);

comment on table extensions is e''
  'External extension APIs the user can call from within Aerie UI.';
comment on column extensions.description is e''
  'An optional description of the external extension.';
comment on column extensions.label is e''
  'The name of the extension that is displayed in the UI.';
comment on column extensions.owner is e''
  'The user who owns the extension.';
comment on column extensions.url is e''
  'The URL of the API to be called.';
comment on column extensions.updated_at is e''
  'The time the extension was last updated.';

create or replace function extensions_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger set_timestamp
  before update on extensions
  for each row
execute function extensions_set_updated_at();
