create table merlin.uploaded_file (
  id integer generated always as identity,
  path bytea not null,

  name text not null,
  created_date timestamptz not null default now(),
  modified_date timestamptz not null default now(),
  deleted_date timestamptz null,

  constraint uploaded_file_synthetic_key
    primary key (id),
  constraint uploaded_file_natural_key
    unique (name)
);

comment on table merlin.uploaded_file is e''
  'A file stored physically in an external filesystem.';

comment on column merlin.uploaded_file.id is e''
  'An opaque internal reference to this file.';
comment on column merlin.uploaded_file.path is e''
  'An opaque external reference to this file in an external filesystem.'
'\n'
  'This is of type bytea since OS paths do not have a set encoding.';
comment on column merlin.uploaded_file.name is e''
  'A human-readable identifier for this file.';
comment on column merlin.uploaded_file.created_date is e''
  'The instant at which this file was added to the datastore.';
comment on column merlin.uploaded_file.modified_date is e''
  'The instant at which this file was last updated.';
comment on column merlin.uploaded_file.deleted_date is e''
  'The instant at which this file was removed from use.'
'\n'
  'Deletion does not remove the file from the external filesystem, '
  'nor does it invalidate pre-existing references to the file. '
  'However, new references should not be made to a file with a non-null deleted_date.';
