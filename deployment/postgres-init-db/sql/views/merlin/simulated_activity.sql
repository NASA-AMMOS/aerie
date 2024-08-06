create view merlin.simulated_activity as
(
  select span.span_id as id,
         sd.id as simulation_dataset_id,
         span.parent_id as parent_id,
         span.start_offset as start_offset,
         span.duration as duration,
         span.attributes as attributes,
         span.type as activity_type_name,
         (span.attributes#>>'{directiveId}')::integer as directive_id,
         sd.simulation_start_time + span.start_offset as start_time,
         sd.simulation_start_time + span.start_offset + span.duration as end_time
   from merlin.span span
     join merlin.dataset d on span.dataset_id = d.id
     join merlin.simulation_dataset sd on d.id = sd.dataset_id
     join merlin.simulation s on s.id = sd.simulation_id
);

comment on view merlin.simulated_activity is e''
  'Concrete activity instance created via simulation.';

comment on column merlin.simulated_activity.id is e''
  'Unique identifier for the activity instance span.';

comment on column merlin.simulated_activity.simulation_dataset_id is e''
  'The simulation dataset this activity is part of.';

comment on column merlin.simulated_activity.parent_id is e''
  'The parent activity of this activity.';

comment on column merlin.simulated_activity.start_offset is e''
  'The offset from the dataset start at which this activity begins.';

comment on column merlin.simulated_activity.duration is e''
  'The amount of time this activity extends for.';

comment on column merlin.simulated_activity.attributes is e''
  'A set of named values annotating this activity.';

comment on column merlin.simulated_activity.activity_type_name is e''
  'The activity type of this activity.';

comment on column merlin.simulated_activity.directive_id is e''
  'The id of the activity directive that created this activity.';

comment on column merlin.simulated_activity.start_time is e''
  'The absolute start time of this activity.';

comment on column merlin.simulated_activity.end_time is e''
  'The absolute end time of this activity.';
