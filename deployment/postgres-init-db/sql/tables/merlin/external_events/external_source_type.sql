-- Create table for external source types
create table merlin.external_source_type (
    name text not null,

    constraint external_source_type_pkey
      primary key (name)
);

comment on table merlin.external_source_type is e''
  'A table for externally imported event source types.';

comment on column merlin.external_source_type.name is e''
  'The identifier for this external_source_type, as well as its name.';
