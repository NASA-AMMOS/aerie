create view merlin.resource_profile as
(
select profile_segment.dataset_id as dataset_id,
       profile_segment.profile_id as profile_id,
       profile_segment.start_offset as start_offset,
       profile_segment.dynamics as dynamics,
       plan.start_time + profile_segment.start_offset as start_time,
       coalesce(
             plan.start_time + (
             select p.start_offset
             from merlin.profile_segment p
             where p.start_offset > profile_segment.start_offset
               and p.profile_id = profile_segment.profile_id
               and p.dataset_id = profile_segment.dataset_id
             order by p.start_offset
             limit 1
           ),
             plan.start_time + plan.duration
         ) as end_time

from merlin.profile_segment
       join merlin.dataset on profile_segment.dataset_id = dataset.id
       left join merlin.plan_dataset pd on dataset.id = pd.dataset_id
       left join merlin.simulation_dataset sd on dataset.id = sd.dataset_id
       left join merlin.simulation s on sd.simulation_id = s.id
       join merlin.plan on plan.id = s.plan_id or plan.id = pd.plan_id
);

comment on view merlin.resource_profile is e''
  'A piece of a profile associated with a dataset, starting at a particular offset from the dataset basis. '
  'The profile is governed at any time T by the latest profile whose start_offset is no later than T.'
  'This view adds in absolute start and end times to the profile segment.';

comment on column merlin.resource_profile.dataset_id is e''
  'The dataset this segment''s profile is a part of.'
  '\n'
  'Denormalized for partitioning. Should always match ''profile.dataset_id''.';

comment on column merlin.resource_profile.profile_id is e''
  'The profile this segment is a part of.';

comment on column merlin.resource_profile.start_offset is e''
  'The offset from the start of the plan at which this profile segment takes over the profile''s behavior.';

comment on column merlin.resource_profile.dynamics is e''
  'A formal description of the behavior of the resource between this segment and the next.'
  '\n'
  'May be NULL if no behavior is known, thereby canceling any prior behavior.';

comment on column merlin.resource_profile.start_time is e''
  'The absolute time this profile segment takes over the profile''s behavior.';

comment on column merlin.resource_profile.end_time is e''
  'The absolute time this profile segment ends influencing the profile''s behavior.';
