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
comment on column sequencing.workspace.owner is e''
  'The user responsible for this workspace.';
comment on column sequencing.workspace.created_at is e''
  'Time the workspace was created at.';
comment on column sequencing.workspace.updated_at is e''
  'Time the workspace was last updated.';
comment on column sequencing.workspace.updated_by is e''
  'THe user who last updated the workspace.';

create trigger set_timestamp
  before update on sequencing.workspace
  for each row
execute function util_functions.set_updated_at();
