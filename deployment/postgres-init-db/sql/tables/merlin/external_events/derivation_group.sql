-- Create a table to represent derivation groups for external sources
CREATE TABLE merlin.derivation_group (
    name text NOT NULL UNIQUE,
    source_type_name text NOT NULL,

    constraint derivation_group_pkey
      primary key (name, source_type_name),
    constraint derivation_group_references_external_source_type
      foreign key (source_type_name)
      references merlin.external_source_type(name)
);

COMMENT ON TABLE merlin.derivation_group IS 'A table to represent the names of groups of sources to run derivation operations over.';
