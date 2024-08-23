-- Create a table to represent external event sources.
create table merlin.external_source (
    key text not null,
    source_type_name text not null,
    derivation_group_name text not null,
    valid_at timestamp with time zone not null,
    start_time timestamp with time zone not null,
    end_time timestamp with time zone not null,
    CHECK (end_time >= start_time),
    created_at timestamp with time zone default now() not null,
    metadata jsonb,

    constraint external_source_pkey
      primary key (key, derivation_group_name),
    -- a given dg cannot have two sources with the same valid_at!
    CONSTRAINT dg_unique_valid_at UNIQUE (derivation_group_name, valid_at),
    constraint external_source_references_external_source_type_name
      foreign key (source_type_name)
      references merlin.external_source_type(name),
    constraint external_source_type_matches_derivation_group
      foreign key (derivation_group_name, source_type_name)
      references merlin.derivation_group (name, source_type_name)
);

comment on table merlin.external_source is e''
  'A table for externally imported event sources.';
