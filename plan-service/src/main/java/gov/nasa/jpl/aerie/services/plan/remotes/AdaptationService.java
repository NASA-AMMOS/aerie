package gov.nasa.jpl.aerie.services.plan.remotes;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;

public interface AdaptationService {
  boolean isMissionModelDefined(String adaptationId);
  List<String> areActivityParametersValid(String adaptationId, SerializedActivity activityParameters) throws NoSuchAdaptationException;
  Pair<Instant, JsonValue> simulatePlan(Plan plan) throws NoSuchAdaptationException;

  class NoSuchAdaptationException extends Exception {}
}
