package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;

import java.util.Map;

public interface AdaptationService {
  Map<String, ActivityType> getActivityTypes(String adaptationId) throws NoSuchAdaptationException;
}
