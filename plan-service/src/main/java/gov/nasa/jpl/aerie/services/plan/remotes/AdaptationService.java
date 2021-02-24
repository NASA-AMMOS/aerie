package gov.nasa.jpl.aerie.services.plan.remotes;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.models.SimulationResults;

import java.util.List;

public interface AdaptationService {
  boolean isMissionModelDefined(String adaptationId);
  List<String> areActivityParametersValid(String adaptationId, SerializedActivity activityParameters) throws NoSuchAdaptationException;
  SimulationResults simulatePlan(Plan plan) throws NoSuchAdaptationException;

  class NoSuchAdaptationException extends Exception {}
}
