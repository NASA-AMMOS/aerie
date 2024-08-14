-- Ensure that added external source's source type match this derivation group's source type; requires a trigger and a function!
CREATE OR REPLACE FUNCTION ensure_source_type_match()
RETURNS TRIGGER AS $$
BEGIN
  -- verify external_source.source_type_name = derivation_group.source_type_name
  PERFORM 1
  FROM merlin.derivation_group
  WHERE derivation_group.id = NEW.derivation_group_id AND derivation_group.source_type_name = NEW.source_type_name;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'The source type from the newly added source and the source type of the derivation group do not match.';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_source_type_match
BEFORE INSERT OR UPDATE ON merlin.external_source
FOR EACH ROW
EXECUTE FUNCTION ensure_source_type_match();
