package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Map;

public final class RemoteAdaptationService implements AdaptationService {
  @Override
  public Map<String, ActivityType> getActivityTypes(final String adaptationId) {
    throw new NotImplementedException("TODO: communicate with remote service");
  }
}
