package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchAdaptationException;

import java.util.Map;

public interface AdaptationService {
  Map<String, Map<String, ParameterSchema>> getActivityTypes(String adaptationId) throws NoSuchAdaptationException;
}
