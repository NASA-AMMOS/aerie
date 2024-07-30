-- Create a view for derivation of events
CREATE OR REPLACE VIEW merlin.derived_events
  AS
  SELECT
      source_id,
      file_id,
      event_id,
      event_key,
      event_type_id,
      derivation_group_id,
      start_time,
      source_range,
      valid_at
     FROM ( SELECT rule1_3.source_id,
              rule1_3.file_id,
              rule1_3.event_id,
              rule1_3.event_key,
              rule1_3.event_type_id,
              rule1_3.derivation_group_id,
              rule1_3.start_time,
              rule1_3.source_range,
              rule1_3.valid_at,
              row_number() OVER (PARTITION BY rule1_3.event_key, rule1_3.derivation_group_id ORDER BY rule1_3.valid_at DESC) AS rn
             FROM ( SELECT sub.id AS source_id,
                      sub.file_id,
                      external_event.id AS event_id,
                      external_event.key AS event_key,
                      external_event.event_type_id,
                      sub.derivation_group_id,
                      external_event.start_time,
                      sub.source_range,
                      sub.valid_at
                     FROM merlin.external_event
                       JOIN (
  	 					WITH derivation_tb_range AS (
  							SELECT id, key, file_id, derivation_group_id, tstzmultirange(tstzrange(start_time, end_time)) as dr, valid_at
  								FROM merlin.external_source
  								ORDER BY valid_at ASC
  						),
  						ranges_with_subs AS (
  							SELECT tr1.id, tr1.file_id, tr1.derivation_group_id, tr1.key, tr1.dr as original_range, COALESCE(array_remove(array_agg(tr2.dr ORDER BY tr2.valid_at) FILTER (WHERE tr1.derivation_group_id = tr2.derivation_group_id), NULL), '{}') as subsequent_ranges, tr1.valid_at
  								FROM derivation_tb_range tr1
  								LEFT JOIN derivation_tb_range tr2 ON tr1.valid_at < tr2.valid_at
  								GROUP BY tr1.id, tr1.derivation_group_id, tr1.key, tr1.file_id, tr1.valid_at, tr1.dr
  						)
  						SELECT id, file_id, derivation_group_id, key, original_range, subsequent_ranges, merlin.subtract_later_ranges(original_range, subsequent_ranges) as source_range, valid_at
  							FROM ranges_with_subs
  							ORDER BY derivation_group_id DESC, valid_at ASC
  	 				) sub ON sub.id = external_event.source_id
                    WHERE sub.source_range @> external_event.start_time
                    ORDER BY sub.derivation_group_id, external_event.start_time) rule1_3) t
    WHERE rn = 1
    ORDER BY start_time;

ALTER VIEW IF EXISTS merlin.derived_events OWNER TO aerie;
