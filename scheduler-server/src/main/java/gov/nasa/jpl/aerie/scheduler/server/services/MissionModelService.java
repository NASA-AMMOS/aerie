package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.models.ActivityType;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.ResourceType;

import java.io.IOException;
import java.util.Collection;

public interface MissionModelService {
  MissionModelTypes getMissionModelTypes(final PlanId planId)
  throws IOException, MissionModelServiceException;
  MissionModelTypes getMissionModelTypes(final MissionModelId missionModelId)
  throws IOException, MissionModelServiceException,
         NoSuchMissionModelException;

  class MissionModelServiceException extends Exception {
    MissionModelServiceException(final String message) {
      super(message);
    }

    public MissionModelServiceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  record MissionModelTypes(Collection<ActivityType> activityTypes, Collection<ResourceType> resourceTypes) {}
}
