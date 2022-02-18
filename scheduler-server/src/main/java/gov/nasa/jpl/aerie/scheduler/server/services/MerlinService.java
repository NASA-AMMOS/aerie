package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.Plan;
import gov.nasa.jpl.aerie.scheduler.Problem;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;

import java.io.IOException;

/**
 * interface for retrieving / storing details to merlin
 */
public interface MerlinService {

  /**
   * fetch current revision number of the target plan stored in aerie
   *
   * @param planId identifier of the plan container whose details should be fetched
   * @return the current revision number of the plan as stored in aerie
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  long getPlanRevision(final PlanId planId) throws IOException, NoSuchPlanException;

  /**
   * fetch current metadata of the target plan (not the activity instance content)
   *
   * @param planId identifier of the plan container whose details should be fetched
   * @return metadata about the plan that is useful to the scheduler, including current plan revision
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  PlanMetadata getPlanMetadata(final PlanId planId) throws IOException, NoSuchPlanException;

  /**
   * create an in-memory snapshot of the target plan's activity contents from aerie
   *
   * @param planMetadata identifying details of the plan to fetch content for
   * @param mission the mission model that the plan adheres to
   * @return a newly allocated snapshot of the plan contents
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  Plan getPlanActivities(final PlanMetadata planMetadata, final Problem mission)
  throws IOException, NoSuchPlanException;

  /**
   * create an entirely new plan container in aerie and synchronize the in-memory plan to it
   *
   * does not mutate the original plan, so metadata remains valid for the original plan
   *
   * @param planMetadata identifying details of a plan to emulate in creating new container. id is ignored.
   * @param plan plan with all activity instances that should be stored to target merlin plan container
   * @return the database id of the newly created aerie plan container
   * @throws NoSuchPlanException when the plan container could not be found in aerie after creation
   */
  PlanId createNewPlanWithActivities(final PlanMetadata planMetadata, final Plan plan)
  throws IOException, NoSuchPlanException;

  /**
   * create a new empty plan container based on specifications
   *
   * does not attach a simulation / configuration to the plan! (may require separate call to createSimulationForPlan)
   *
   * @param name the human legible label for the new plan container to create
   * @param modelId the database identifier of the mission model to associate with the plan
   * @param startTime the absolute start time of the new plan container
   * @param duration the duration of the new plan container
   * @return the database id of the newly created aerie plan container
   * @throws NoSuchPlanException when the plan container could not be found in aerie after creation
   */
  PlanId createEmptyPlan(final String name, final long modelId, final Time startTime, final Duration duration)
  throws IOException, NoSuchPlanException;


  /**
   * create a new empty simulation container with default configuration args attached to the target plan
   *
   * @param planId the database id of the aerie plan container to attach the simulation container to
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  void createSimulationForPlan(final PlanId planId) throws IOException, NoSuchPlanException;

  /**
   * synchronize the in-memory plan back over to aerie data stores via update operations
   *
   * the plan revision will change!
   *
   * @param planId aerie database identifier of the target plan to synchronize into
   * @param plan plan with all activity instances that should be stored to target merlin plan container
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  void updatePlanActivities(final PlanId planId, final Plan plan) throws IOException, NoSuchPlanException;

  /**
   * confirms that the specified plan exists in the aerie database, throwing exception if not
   *
   * @param planId the target plan database identifier
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  //TODO: (defensive) should combine such checks into the mutations they are guarding, but not possible in graphql?
  void ensurePlanExists(final PlanId planId) throws IOException, NoSuchPlanException;

  /**
   * delete all the activity instances stored in the target plan container
   *
   * the plan revision will change!
   *
   * @param planId the database id of the plan container to clear
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  void clearPlanActivities(final PlanId planId) throws IOException, NoSuchPlanException;

  /**
   * create activity instances in the target plan container for each activity in the input plan
   *
   * does not attempt to resolve id clashes or do activity instance updates
   *
   * the plan revision will change!
   *
   * @param planId the database id of the plan container to populate with new activity instances
   * @param plan the plan from which to copy all activity instances into aerie
   * @throws NoSuchPlanException when the plan container does not exist in aerie
   */
  void createAllPlanActivities(final PlanId planId, final Plan plan) throws IOException, NoSuchPlanException;


}
