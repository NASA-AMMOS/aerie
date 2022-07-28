create table profile_segment_stream (
  dataset_id integer not null,
  profile_id integer not null,
  timeline_segment_id integer not null,

  dynamics jsonb null,

  constraint profile_segment_natural_key
    unique (dataset_id, profile_id, timeline_segment_id),

  constraint profile_segment_owned_by_profile
    foreign key (profile_id)
    references profile
    on update cascade
    on delete cascade,

  constraint segment_is_streamed
    foreign key (timeline_segment_id)
    references timeline_segment (id)
    on update cascade
    on delete cascade
)
partition by list (dataset_id);

comment on table profile_segment_stream is e''
  'A profile_segment_stream represents a subset of the information in profile_segment that is pertinent to being streamed out'
'\n'
  'Notably, it lacks any sort of start_offset or temporal reference. This is because profile_segment_stream is wrapped in a sense by timeline_segment'
'\n'
  'i.e. a timeline_segment will contain a new simulation_time, which all wrapped profile_segment_stream are assumed to start at.'
'\n'
  'The duration of said profile_segments is simply the amount of simulation time that has passed inbetween profile_segment_streams for a given profile'
'\n'
  'If a timeline_segment doesn't include a profile_segment_stream for a given profile, it should extrapolate the dynamics of the previous profile_segment_stream up to the new provided simulation_time'

comment on column profile_segment_stream.timeline_segment_id is e''
  'Relates this profile segment to a timeline segment that streams this data out'

comment on column profile_segment_stream.profile_id is e''
  'The profile this segment is a part of.';

comment on column profile_segment_stream.dynamics is e''
  'A formal description of the behavior of the resource between this segment and the next.'
'\n'
  'May be NULL if no behavior is known, thereby canceling any prior behavior.';
