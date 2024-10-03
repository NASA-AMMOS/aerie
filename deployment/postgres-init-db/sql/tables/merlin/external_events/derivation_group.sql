create table merlin.derivation_group (
    name text not null unique,
    source_type_name text not null,

    constraint derivation_group_pkey
      primary key (name),
    constraint derivation_group_references_external_source_type
      foreign key (source_type_name)
      references merlin.external_source_type(name)
);

comment on table merlin.derivation_group is e''
  'A representation of the names of groups of sources to run derivation operations over.';

-- TODO: make name the pk on its own??
comment on column merlin.derivation_group.name is e''
  'The name and primary key of the derivation group.';
comment on column merlin.derivation_group.source_type_name is e''
  'The name of the external_source_type of sources in this derivation group.';
