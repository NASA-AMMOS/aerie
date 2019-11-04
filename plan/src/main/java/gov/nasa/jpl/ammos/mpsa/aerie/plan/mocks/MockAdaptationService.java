package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MockAdaptationService implements AdaptationService {
  private final Map<String, Map<String, Map<String, ParameterSchema>>> adaptations = new HashMap<>();
  private int nextId = 0;

  @Override
  public Map<String, Map<String, ParameterSchema>> getActivityTypes(final String adaptationId) throws NoSuchAdaptationException {
    final Map<String, Map<String, ParameterSchema>> activityTypes = this.adaptations.get(adaptationId);
    if (activityTypes == null) {
      throw new NoSuchAdaptationException(adaptationId);
    }

    return activityTypes;
  }

  public String addAdaptation(final Map<String, Map<String, ParameterSchema>> activityTypes) {
    final String adaptationId = Objects.toString(this.nextId++);

    final Map<String, Map<String, ParameterSchema>> clonedActivityTypes = new HashMap<>();
    for (final var entry : activityTypes.entrySet()) {
      clonedActivityTypes.put(entry.getKey(), new HashMap<>(entry.getValue()));
    }
    adaptations.put(adaptationId, clonedActivityTypes);

    return adaptationId;
  }
}
