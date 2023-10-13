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
  language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger extensions_set_timestamp
  before update on extensions
  for each row
execute function extensions_set_updated_at();

create table extension_roles (
  extension_id  integer not null,
  id integer generated always as identity,
  role text not null,

  constraint extension_roles_primary_key primary key (id),
  constraint extension_roles_to_extension
    foreign key (extension_id)
      references "extensions"
      on delete cascade
);

comment on table extension_roles is e''
  'A mapping of extensions to what roles can access them.';
comment on column extension_roles.extension_id is e''
  'The extension that the role is defined for.';
comment on column extension_roles.role is e''
  'The role that is allowed to access the extension.';

call migrations.mark_migration_applied('2');
