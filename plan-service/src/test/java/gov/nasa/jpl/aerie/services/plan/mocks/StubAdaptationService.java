package gov.nasa.jpl.aerie.services.plan.mocks;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.models.SimulationResults;
import gov.nasa.jpl.aerie.services.plan.remotes.AdaptationService;

import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StubAdaptationService implements AdaptationService {
  public static final String NONEXISTENT_ADAPTATION_ID = "nonexistent adaptation";
  public static final String EXISTENT_ADAPTATION_ID = "existent adaptation";

  public static final String NONEXISTENT_ACTIVITY_TYPE_ID = "nonexistent activity type";
  public static final String EXISTENT_ACTIVITY_TYPE_ID = "existent activity type";

  public static final SerializedActivity VALID_ACTIVITY_PARAMETERS = new SerializedActivity(EXISTENT_ACTIVITY_TYPE_ID, Map.of());

  @Override
  public boolean isMissionModelDefined(final String adaptationId) {
    return (Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID));
  }

  @Override
  public List<String> areActivityParametersValid(final String adaptationId, final SerializedActivity activityParameters) throws NoSuchAdaptationException {
    if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) throw new NoSuchAdaptationException();

    if (!Objects.equals(activityParameters.getTypeName(), EXISTENT_ACTIVITY_TYPE_ID)) {
      return List.of("unknown activity type");
    } else if (!Objects.equals(activityParameters, VALID_ACTIVITY_PARAMETERS)) {
      return List.of("invalid activity parameters");
    } else {
      return List.of();
    }
  }

  @Override
  public SimulationResults simulatePlan(final Plan plan) throws NoSuchAdaptationException {
    if (!Objects.equals(plan.adaptationId, EXISTENT_ADAPTATION_ID)) throw new NoSuchAdaptationException();

    return new SimulationResults(Instant.EPOCH, Map.of(), JsonValue.EMPTY_JSON_ARRAY, JsonValue.EMPTY_JSON_OBJECT);
  }
}
