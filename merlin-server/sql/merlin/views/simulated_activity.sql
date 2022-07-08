create view simulated_activity as
(
  select span.id as id,
         simulation_dataset.id as simulation_dataset_id,
         span.parent_id as parent_id,
         span.start_offset as start_offset,
         span.duration as duration,
         span.attributes as attributes,
         span.type as activity_type_name,
         (span.attributes#>>'{directiveId}')::integer as directive_id,
         plan.start_time + span.start_offset as start_time,
         plan.start_time + span.start_offset + span.duration as end_time


   from span
     join dataset on span.dataset_id = dataset.id
     join simulation_dataset on dataset.id = simulation_dataset.dataset_id
     join simulation on simulation.id = simulation_dataset.simulation_id
     join plan on plan.id = simulation.plan_id
);

comment on view simulated_activity is e''
  'Concrete activity instance created via simulation.';

comment on column simulated_activity.id is e''
  'Unique identifier for the activity instance span.';

comment on column simulated_activity.simulation_dataset_id is e''
  'The simulation dataset this activity is part of.';

comment on column simulated_activity.parent_id is e''
  'The parent activity of this activity.';

comment on column simulated_activity.start_offset is e''
  'The offset from the dataset start at which this activity begins.';

comment on column simulated_activity.duration is e''
  'The amount of time this activity extends for.';

comment on column simulated_activity.attributes is e''
  'A set of named values annotating this activity.';

comment on column simulated_activity.activity_type_name is e''
  'The activity type of this activity.';

comment on column simulated_activity.directive_id is e''
  'The id of the activity directive that created this activity.';

comment on column simulated_activity.start_time is e''
  'The absolute start time of this activity.';

comment on column simulated_activity.end_time is e''
  'The absolute end time of this activity.';
