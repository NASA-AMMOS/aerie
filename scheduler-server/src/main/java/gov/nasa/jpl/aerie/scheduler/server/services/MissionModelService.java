package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

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

  record ActivityType(String name, Map<String, ValueSchema> parameters) {}

  record ResourceType(String name, String type, ValueSchema schema) {}

  record MissionModelTypes(Collection<ActivityType> activityTypes, Collection<ResourceType> resourceTypes) {}
}
