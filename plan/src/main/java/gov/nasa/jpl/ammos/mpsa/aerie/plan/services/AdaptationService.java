package gov.nasa.jpl.ammos.mpsa.aerie.plan.services;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;

import java.util.Map;

public interface AdaptationService {
    Map<String, ActivityType> getActivityTypes(String adaptationId);
}
