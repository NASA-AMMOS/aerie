create table activity_directive_metadata_schema (
  key text not null primary key,
  schema jsonb not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table activity_directive_metadata_schema is 'e'
  'Schema for the activity directive metadata.';
comment on column activity_directive_metadata_schema.key is 'e'
  'Key of the metadata.';
comment on column activity_directive_metadata_schema.schema is 'e'
  'Schema of the metadata field.';
comment on column activity_directive_metadata_schema.created_at is 'e'
  'Timestamp when the metadata field was created.';
comment on column activity_directive_metadata_schema.updated_at is 'e'
  'Timestamp when the metadata field was last updated.';

create or replace function activity_directive_metadata_schema_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger activity_directive_metadata_schema_updated_at_trigger
before update
on activity_directive_metadata_schema
for each row
execute procedure activity_directive_metadata_schema_updated_at();
