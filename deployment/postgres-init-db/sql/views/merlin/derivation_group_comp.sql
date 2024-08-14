CREATE OR REPLACE VIEW merlin.derivation_group_comp
 AS
SELECT derivation_group.id,
   derivation_group.name,
   derivation_group.source_type_name,
   array_agg(DISTINCT concat(sources.source_id, ', ', sources.key, ', ', sources.contained_events)) AS sources,
   array_agg(DISTINCT sources.type) AS event_types,
   count(DISTINCT c.event_key) AS derived_total
  FROM merlin.derivation_group
    LEFT JOIN ( SELECT external_source.id AS source_id,
           external_source.key,
           count(a.event_key) AS contained_events,
		   external_event_type.name AS type,
           external_source.derivation_group_id,
           external_source.valid_at
          FROM merlin.external_source
            JOIN ( SELECT external_event.source_id,
					external_event.event_type_name,
                   external_event.key AS event_key
                  FROM merlin.external_event) a ON a.source_id = external_source.id
			JOIN merlin.external_event_type ON external_event_type.name = a.event_type_name
         GROUP BY external_source.id, external_source.key, external_source.derivation_group_id, external_source.valid_at, type) sources ON sources.derivation_group_id = derivation_group.id
    LEFT JOIN ( SELECT derived_events.event_key,
           derived_events.derivation_group_id
          FROM merlin.derived_events) c ON c.derivation_group_id = derivation_group.id
 GROUP BY derivation_group.id, derivation_group.name, derivation_group.source_type_name;

ALTER VIEW IF EXISTS merlin.derivation_group_comp OWNER TO aerie;
COMMENT ON VIEW merlin.derivation_group_comp
    IS 'A view detailing all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. As such, a new view was created to lighten the load.';
