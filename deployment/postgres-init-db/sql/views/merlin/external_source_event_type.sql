-- Create table mapping external sources to their contained event types
CREATE OR REPLACE VIEW merlin.external_source_event_type
 AS
 SELECT external_source.id AS external_source_id,
    array_agg(DISTINCT external_event.event_type_id) AS event_type_ids,
    array_agg(DISTINCT external_event_type.name) AS event_types
   FROM merlin.external_source
     JOIN merlin.external_event ON external_event.source_id = external_source.id
     JOIN merlin.external_event_type ON external_event_type.id = external_event.event_type_id
  GROUP BY external_source.id;

ALTER VIEW IF EXISTS merlin.external_source_event_type OWNER TO aerie;
COMMENT ON VIEW merlin.external_source_event_type
    IS 'Should be deprecated with the introduction of strict external source schemas, dictating allowable event types for given source types. But for now, this will do.';
