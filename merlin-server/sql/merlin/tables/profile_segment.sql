create table profile_segment (
  dataset_id integer not null,
  profile_id integer not null,

  start_offset interval not null,
  dynamics jsonb null,
  is_gap bool not null default false,

  constraint profile_segment_natural_key
    unique (dataset_id, profile_id, start_offset),
  constraint profile_segment_owned_by_profile
    foreign key (profile_id)
    references profile
    on update cascade
    on delete cascade
)
partition by list (dataset_id);

-- TODO: Add a range index for start_offset.

comment on table profile_segment is e''
  'A piece of a profile associated with a dataset, starting at a particular offset from the dataset basis. '
  'The profile is governed at any time T by the latest profile whose start_offset is no later than T.'
'\n'
  'The table is partitioned by dataset, so all profiles for a dataset can be managed in bulk.'
'\n'
  'Design note: We expect most profiles to be dense (i.e. have few to no internal regions with unknown behavior), '
  'so defining the segment duration implicitly by whenever the next segment begins avoids redundancy. '
  'In exchange, a trailing NULL segment is necessary if the effective end of a profile must be identified.';

comment on column profile_segment.dataset_id is e''
  'The dataset this segment''s profile is a part of.'
'\n'
  'Denormalized for partitioning. Should always match ''profile.dataset_id''.';
comment on column profile_segment.profile_id is e''
  'The profile this segment is a part of.';
comment on column profile_segment.start_offset is e''
  'The offset from the start of the plan at which this profile segment takes over the profile''s behavior.';
comment on column profile_segment.dynamics is e''
  'A formal description of the behavior of the resource between this segment and the next.'
'\n'
  'May be NULL if no behavior is known, thereby canceling any prior behavior.';
comment on column profile_segment.is_gap is e''
  'Whether this segment has a value. If not, the value is not used, and is treated as unknown.';
