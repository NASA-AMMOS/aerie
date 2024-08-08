---- Add the new workspace table

create table sequencing.workspace (
  id integer generated always as identity,

  name text not null,

  created_at timestamptz not null default now(),
  owner text,
  updated_at timestamptz not null default now(),
  updated_by text,

  constraint workspace_synthetic_key
    primary key (id),
  foreign key (owner)
    references permissions.users
    on update cascade
    on delete set null,
  foreign key (updated_by)
    references permissions.users
    on update cascade
    on delete set null
);

comment on table sequencing.workspace is e''
  'A container for multiple sequences.';
comment on column sequencing.workspace.name is e''
  'The name of the workspace.';

create trigger set_timestamp
  before update on sequencing.workspace
  for each row
execute function util_functions.set_updated_at();

---- Add the new workspace_id column to the user_sequence table

alter table sequencing.user_sequence
  add column workspace_id integer,

  add foreign key (workspace_id)
    references sequencing.parcel (id)
    on delete cascade;

comment on column sequencing.user_sequence.workspace_id is e''
  'The workspace the sequence is associated with.';

---- Populate the workspace table with a default workspace to contain existing sequences

insert into sequencing.workspace (name, owner)
values ('Workspace 1', 'Aerie Legacy');

update sequencing.user_sequence
  set workspace_id = 1;

call migrations.mark_migration_applied('9');
