create table profile_segment (
  dataset_id integer not null,
  profile_id integer not null,

  start_offset interval not null,
  dynamics jsonb null,
  is_gap bool not null default false,

  constraint profile_segment_natural_key
    unique (dataset_id, profile_id, start_offset)
)
partition by list (dataset_id);

-- TODO: Add a database range index for start_offset for efficient time searching.

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
  'The offset from the dataset start time at which this profile segment takes over the profile''s behavior.';
comment on column profile_segment.dynamics is e''
  'A formal description of the behavior of the resource between this segment and the next.'
'\n'
  'May be NULL if no behavior is known, thereby canceling any prior behavior.';
comment on column profile_segment.is_gap is e''
  'Whether this segment has a value. If not, the value is not used, and is treated as unknown.';

create function profile_segment_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(
    select from profile
      where profile.dataset_id = new.dataset_id
      and profile.id = new.profile_id
    for key share of profile)
    -- for key share is important: it makes sure that concurrent transactions cannot update
    -- the columns that compose the profile's key until after this transaction commits.
  then
    raise exception 'foreign key violation: there is no profile with id % in dataset %', new.profile_id, new.dataset_id;
  end if;
  return new;
end$$;

comment on function profile_segment_integrity_function is e''
  'Used to simulate a foreign key constraint between profile_segment and profile, to avoid acquiring a lock on the'
  'profile table when creating a new partition of profile_segment. This function checks that a corresponding profile'
  'exists for every inserted or updated profile_segment. A trigger that calls this function is added separately to each'
  'new partition of profile_segment.';

create constraint trigger insert_update_profile_segment_trigger
  after insert or update on profile_segment
  for each row
execute function profile_segment_integrity_function();
