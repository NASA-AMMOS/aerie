package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;

import java.io.IOException;

public interface MissionModelService {
  TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final PlanId planId)
  throws IOException, MissionModelServiceException;
  TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final MissionModelId missionModelId)
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
}
