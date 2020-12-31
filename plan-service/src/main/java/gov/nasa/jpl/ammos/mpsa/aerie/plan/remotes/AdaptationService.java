package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.SimulationResults;

import java.util.List;

public interface AdaptationService {
  boolean isMissionModelDefined(String adaptationId);
  List<String> areActivityParametersValid(String adaptationId, SerializedActivity activityParameters) throws NoSuchAdaptationException;
  SimulationResults simulatePlan(Plan plan, long samplingPeriod) throws NoSuchAdaptationException;

  class NoSuchAdaptationException extends Exception {}
}
