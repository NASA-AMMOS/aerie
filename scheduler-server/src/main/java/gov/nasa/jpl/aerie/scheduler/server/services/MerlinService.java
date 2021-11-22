package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;

/**
 * interface for retrieving / storing details to merlin
 */
public interface MerlinService {

  /**
   * fetch current metadata of the target plan (not the activity instance content)
   *
   * @param planId identifier of the plan container whose details should be fetched
   * @return metadata about the plan that is useful to the scheduler, including current plan revision
   * @throws NoSuchPlanException when the plan container does not exist
   */
  PlanMetadata getPlanMetadata(String planId) throws NoSuchPlanException;
}
