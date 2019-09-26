package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MockAdaptationService implements AdaptationService {
  private final Map<String, Map<String, ActivityType>> adaptations = new HashMap<>();
  private int nextId = 0;

  @Override
  public Map<String, ActivityType> getActivityTypes(final String adaptationId) throws NoSuchAdaptationException {
    final Map<String, ActivityType> activityTypes = this.adaptations.get(adaptationId);
    if (activityTypes == null) {
      throw new NoSuchAdaptationException(adaptationId);
    }

    return activityTypes;
  }

  public String addAdaptation(final Map<String, ActivityType> activityTypes) {
    final String adaptationId = Objects.toString(this.nextId++);

    final Map<String, ActivityType> clonedActivityTypes = new HashMap<>();
    for (final var entry : activityTypes.entrySet()) {
      clonedActivityTypes.put(entry.getKey(), new ActivityType(entry.getValue()));
    }
    adaptations.put(adaptationId, clonedActivityTypes);

    return adaptationId;
  }
}
