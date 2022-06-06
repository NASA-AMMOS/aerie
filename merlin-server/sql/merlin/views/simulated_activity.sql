create view simulated_activity as
(
  select span.id as id,
         simulation_dataset.id as simulation_dataset_id,
         span.parent_id as parent_id,
         span.start_offset as start_offset,
         span.duration as duration,
         span.attributes as attributes,
         span.type as activity_type_name,
         (span.attributes#>>'{directiveId}')::integer as directive_id

   from span
     join dataset on span.dataset_id = dataset.id
     join simulation_dataset on dataset.id = simulation_dataset.dataset_id
);
