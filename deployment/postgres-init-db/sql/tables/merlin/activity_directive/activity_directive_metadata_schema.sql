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

create or replace function validate_activity_directive_metadata_schema()
  returns trigger
  security definer
  language plpgsql as $$
  declare
    _type text;
    _enumerates jsonb;
  begin
    _type := new.schema->>'type';
    if _type is null then
      raise exception 'Invalid metadata schema for key %. It must be an object with a a "type" field.', new.key;
    end if;
    if _type = 'enum' then
      _enumerates := new.schema->'enumerates';
      if _enumerates is null then
        raise exception 'Invalid metadata schema for key %. An enum type must specify an "enumerates" field with enumerated values.', new.key;
      end if;
    elsif _type = 'enum_multiselect' then
      _enumerates := new.schema->'enumerates';
      if _enumerates is null then
        raise exception 'Invalid metadata schema for key %. An enum_multiselect type must specify an "enumerates" field with enumerated values.', new.key;
      end if;
    elsif _type = 'boolean' then
    elsif _type = 'number' then
    elsif _type = 'string' then
    elsif _type = 'long_string' then
    else
      raise exception 'Invalid metadata schema type for key %. It must be one of ["enum", "enum_multiselect", "boolean", "number", "string", "long_string"]. Found: %', new.key, _type;
    end if;
    return new;
  end
$$;

create trigger validate_activity_directive_metadata_schema_trigger
before insert or update on activity_directive_metadata_schema
for each row
execute function validate_activity_directive_metadata_schema();

comment on trigger validate_activity_directive_metadata_schema_trigger on activity_directive_metadata_schema is 'e'
  'Trigger to validate the metadata schema entries for the activity directive metadata.';

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
