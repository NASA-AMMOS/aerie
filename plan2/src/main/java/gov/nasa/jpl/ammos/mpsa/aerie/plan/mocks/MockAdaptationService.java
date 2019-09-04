package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MockAdaptationService implements AdaptationService {
  private final Map<String, MockAdaptation> adaptations = new HashMap<>();
  private int nextId = 0;

  @Override
  public Optional<Adaptation> getAdaptationById(final String adaptationId) {
    return Optional.ofNullable(this.adaptations.get(adaptationId));
  }

  public String addAdaptation(final Map<String, ActivityType> activityTypes) {
    final String adaptationId = Objects.toString(this.nextId++);

    final Map<String, ActivityType> clonedActivityTypes = new HashMap<>();
    for (final var entry : activityTypes.entrySet()) {
      clonedActivityTypes.put(entry.getKey(), new ActivityType(entry.getValue()));
    }
    adaptations.put(adaptationId, new MockAdaptation(clonedActivityTypes));

    return adaptationId;
  }

  private final static class MockAdaptation implements AdaptationService.Adaptation {
    private final Map<String, ActivityType> activityTypes;

    public MockAdaptation(final Map<String, ActivityType> activityTypes) {
      this.activityTypes = activityTypes;
    }

    @Override
    public Map<String, ActivityType> getActivityTypes() {
      final Map<String, ActivityType> clonedActivityTypes = new HashMap<>();
      for (final var entry : this.activityTypes.entrySet()) {
        clonedActivityTypes.put(entry.getKey(), new ActivityType(entry.getValue()));
      }
      return clonedActivityTypes;
    }
  }
}
