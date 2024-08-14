-- Add trigger to manually clean up empty plan_derivation_group links, derivation_groups, external_source_types,
--    and external_event_types (in that order) on source delete. Note that external_event and external_source_event_types
--    automatically cascade source deletions, so manual intervention is only necessary for these 4.
-- TODO: SHOULD THIS BE ADDED TO FUNCTIONS.YAML? IT ISN'T A BAD IDEA TO BE ABLE TO CALL THIS FROM HASURA, BUT DOES THAT
--        VIOLATE THE DESIGN SOMEHOW?
CREATE OR REPLACE FUNCTION merlin.cleanup_on_external_source_delete()
	returns trigger
	language plpgsql
	as
	$$
	begin
		-- STEP 1: DELETE LINGERING plan->dg links:
		WITH to_delete AS (
			SELECT plan_derivation_group.id FROM merlin.plan_derivation_group
				LEFT JOIN merlin.derivation_group ON derivation_group.id = plan_derivation_group.derivation_group_id
				LEFT JOIN merlin.external_source ON derivation_group.source_type_name = external_source.source_type_name
				WHERE key IS NULL
		)
		DELETE FROM merlin.plan_derivation_group
			WHERE id IN (SELECT id FROM to_delete);
		-- STEP 2: DELETE LINGERING DGs:
		WITH to_delete AS (
			SELECT derivation_group.id, name FROM merlin.derivation_group
				LEFT JOIN merlin.external_source ON derivation_group.source_type_name = external_source.source_type_name
				WHERE key IS NULL
		)
		DELETE FROM merlin.derivation_group
			WHERE id IN (SELECT id FROM to_delete);
		-- STEP 3: DELETE LINGERING EXTERNAL SOURCE TYPES
		WITH to_delete AS (
			SELECT external_source_type.id, name from merlin.external_source_type
				LEFT JOIN merlin.external_source ON external_source_type.name = external_source.source_type_name
				WHERE key IS NULL
		)
		DELETE FROM merlin.external_source_type
			WHERE id IN (SELECT id FROM to_delete);
		-- STEP 4: DELETE LINGERING EXTERNAL_EVENT_TYPES
		WITH to_delete AS (
			SELECT external_event_type.name, name FROM merlin.external_event_type
				LEFT JOIN merlin.external_event ON external_event.event_type_name = external_event_type.name
				WHERE key IS NULL
		)
		DELETE FROM merlin.external_event_type
			WHERE name IN (SELECT name FROM to_delete);
		-- STEP 5: RETURN
		RETURN NULL;
	end;
	$$;
ALTER FUNCTION merlin.cleanup_on_external_source_delete()
    OWNER TO aerie;
GRANT EXECUTE ON FUNCTION merlin.cleanup_on_external_source_delete() TO aerie;
CREATE OR REPLACE TRIGGER cleanup_on_external_source_delete
AFTER DELETE ON merlin.external_source
	FOR EACH ROW EXECUTE FUNCTION cleanup_on_external_source_delete();
--ALTER TRIGGER cleanup_on_external_source_delete
--    OWNER TO aerie;
--GRANT EXECUTE ON TRIGGER cleanup_on_external_source_delete TO aerie;
