package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;

import java.util.Map;
import java.util.Optional;

public interface AdaptationService {
  Optional<Adaptation> getAdaptationById(String adaptationId);

  interface Adaptation {
    Map<String, ActivityType> getActivityTypes();
  }
}
