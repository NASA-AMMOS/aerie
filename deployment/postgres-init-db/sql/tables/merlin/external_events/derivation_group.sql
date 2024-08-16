-- Create a table to represent derivation groups for external sources
create table merlin.derivation_group (
    name text not null unique,
    source_type_name text not null,

    constraint derivation_group_pkey
      primary key (name, source_type_name),
    constraint derivation_group_references_external_source_type
      foreign key (source_type_name)
      references merlin.external_source_type(name)
);
comment on table merlin.derivation_group is e''
  'A table to represent the names of groups of sources to run derivation operations over.';
