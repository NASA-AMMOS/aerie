create table merlin.external_source (
    key text not null,
    source_type_name text not null,
    derivation_group_name text not null,
    valid_at timestamp with time zone not null,
    start_time timestamp with time zone not null,
    end_time timestamp with time zone not null,
    CHECK (end_time > start_time),
    created_at timestamp with time zone default now() not null,
    metadata jsonb,

    constraint external_source_pkey
      primary key (key, derivation_group_name),
    -- a given dg cannot have two sources with the same valid_at!
    CONSTRAINT dg_unique_valid_at UNIQUE (derivation_group_name, valid_at),
    -- TODO: going forward, we might want to consider making an exception to the above if sources have no overlap. That
    --        being said, this may be overkill or an unnecessary complication to the general rule.
    constraint external_source_references_external_source_type_name
      foreign key (source_type_name)
      references merlin.external_source_type(name),
    constraint external_source_type_matches_derivation_group
      foreign key (derivation_group_name, source_type_name)
      references merlin.derivation_group (name, source_type_name)
);

comment on table merlin.external_source is e''
  'A table for externally imported event sources.';

comment on column merlin.external_source.key is e''
  'The key, or name, of the external_source.\n'
  'Part of the primary key, along with the derivation_group_name';
comment on column merlin.external_source.source_type_name is e''
  'The type of this external_source.';
comment on column merlin.external_source.derivation_group_name is e''
  'The name of the derivation_group that this external_source is included in.';
comment on column merlin.external_source.valid_at is e''
  'The time (in _planner_ time, NOT plan time) at which a source becomes valid.\n'
  'This time helps determine when a source''s events are valid for the span of time it covers.';
comment on column merlin.external_source.start_time is e''
  'The start time (in _plan_ time, NOT planner time), of the range that this source describes.';
comment on column merlin.external_source.end_time is e''
  'The end time (in _plan_ time, NOT planner time), of the range that this source describes.';
comment on column merlin.external_source.created_at is e''
  'The time (in _planner_ time, NOT plan time) that this particular source was created.\n'
  'This column is used primarily for documentation purposes, and has no associated functionality.';
comment on column merlin.external_source.metadata is e''
  'Any metadata or additional data associated with this version that a data originator may have wanted included.\n'
  'Like the ''created_at'' column, this column is used primarily for documentation purposes, and has no associated functionality.';

