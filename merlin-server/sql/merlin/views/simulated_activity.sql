create view simulated_activity as
(
  select span.id as simulated_activity_id,
         span.dataset_id as dataset_id,
         span.parent_id as parent_simulated_activity_id,
         span.start_offset as start_offset,
         span.duration as duration,
         span.attributes as attributes,
         plan.id as plan_id,
         activity.id as activity_id,
         activity_type.model_id as model_id

   from span
     join dataset on span.dataset_id = dataset.id
     join simulation_dataset on dataset.id = simulation_dataset.dataset_id
     join simulation on simulation_dataset.simulation_id = simulation.id
     join plan on simulation.plan_id = plan.id
     left join activity on (span.attributes #> '{directiveId}')::int = activity.id
     join activity_type on activity_type.name = span.type and activity_type.model_id = plan.model_id
);
