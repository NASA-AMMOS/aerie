package gov.nasa.jpl.aerie.scheduler.simulation;

import com.google.common.collect.MoreCollectors;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimulationFacadeUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationFacadeUtils.class);
  private static int itSimActivityId = 0;

  public static SimulationFacade.PlanSimCorrespondence scheduleFromPlan(final Plan plan, final SchedulerModel schedulerModel){
    final var activities = plan.getActivities();
    final var planActDirectiveIdToSimulationActivityDirectiveId = new DualHashBidiMap<SchedulingActivityDirectiveId, ActivityDirectiveId>();
    if(activities.isEmpty()) return new SimulationFacade.PlanSimCorrespondence(new DualHashBidiMap<>(), Map.of());
    //filter out child activities
    final var activitiesWithoutParent = activities.stream().filter(a -> a.topParent() == null).toList();
    final Map<ActivityDirectiveId, ActivityDirective> directivesToSimulate = new HashMap<>();

    for(final var activity : activitiesWithoutParent){
      final var activityIdSim = new ActivityDirectiveId(itSimActivityId++);
      planActDirectiveIdToSimulationActivityDirectiveId.put(activity.getId(), activityIdSim);
    }

    for(final var activity : activitiesWithoutParent) {
      final var activityDirective = schedulingActToActivityDir(activity, planActDirectiveIdToSimulationActivityDirectiveId, schedulerModel);
      directivesToSimulate.put(
          planActDirectiveIdToSimulationActivityDirectiveId.get(activity.getId()),
          activityDirective);
    }
    return new SimulationFacade.PlanSimCorrespondence(planActDirectiveIdToSimulationActivityDirectiveId, directivesToSimulate);
  }

  /**
   * For activities that have a null duration (in an initial plan for example) and that have been simulated, we pull the duration and
   * replace the original instance with a new instance that includes the duration, both in the plan and the simulation facade
   */
  public static void pullActivityDurationsIfNecessary(
      final Plan plan,
      final SimulationFacade.PlanSimCorrespondence correspondence,
      final SimulationEngine.SimulationActivityExtract activityExtract
  ) {
    final var toReplace = new HashMap<SchedulingActivityDirective, SchedulingActivityDirective>();
    for (final var activity : plan.getActivities()) {
      if (activity.duration() == null) {
        final var activityDirective = findSimulatedActivityById(
            activityExtract.simulatedActivities().values(),
            correspondence.planActDirectiveIdToSimulationActivityDirectiveId().get(activity.getId()));
        if (activityDirective.isPresent()) {
          final var replacementAct = SchedulingActivityDirective.copyOf(
              activity,
              activityDirective.get().duration()
          );
          toReplace.put(activity, replacementAct);
        }
        //if not, maybe the activity is not finished
      }
    }
    toReplace.forEach(plan::replaceActivity);
  }

  private static Optional<SimulatedActivity> findSimulatedActivityById(
      Collection<SimulatedActivity> simulatedActivities,
      final ActivityDirectiveId activityDirectiveId
  ){
    return simulatedActivities.stream()
                              .filter(a -> a.directiveId().isPresent() && a.directiveId().get().equals(activityDirectiveId))
                              .findFirst();
  }

  public static void updatePlanWithChildActivities(
      final SimulationEngine.SimulationActivityExtract activityExtract,
      final Map<String, ActivityType> activityTypes,
      final Plan plan,
      final SimulationFacade.PlanSimCorrespondence planSimCorrespondence,
      final PlanningHorizon planningHorizon)
  {
    //remove all activities with parents
    final var toRemove = plan.getActivities().stream().filter(a -> a.topParent() != null).toList();
    toRemove.forEach(plan::remove);
    //pull child activities
    activityExtract.simulatedActivities().forEach( (activityInstanceId, activity) -> {
      if (activity.parentId() == null) return;
      final var rootParent = getIdOfRootParent(activityExtract, activityInstanceId);
      if(rootParent.isPresent()) {
        final var activityInstance = SchedulingActivityDirective.of(
            activityTypes.get(activity.type()),
            planningHorizon.toDur(activity.start()),
            activity.duration(),
            activity.arguments(),
            planSimCorrespondence.planActDirectiveIdToSimulationActivityDirectiveId().getKey(rootParent.get()),
            null,
            true);
        plan.add(activityInstance);
      }
    });
    //no need to replace in Evaluation because child activities are not referenced in it
  }

  private static Optional<ActivityDirectiveId> getIdOfRootParent(
      final SimulationEngine.SimulationActivityExtract results,
      final SimulatedActivityId instanceId){
    if(!results.simulatedActivities().containsKey(instanceId)){
      if(!results.unfinishedActivities().containsKey(instanceId)){
        LOGGER.debug("The simulation of the parent of activity with id "+ instanceId.id() + " has been finished");
      }
      return Optional.empty();
    }
    final var act = results.simulatedActivities().get(instanceId);
    if(act.parentId() == null){
      // SAFETY: any activity that has no parent must have a directive id.
      return Optional.of(act.directiveId().get());
    } else {
      return getIdOfRootParent(results, act.parentId());
    }
  }

  public static Optional<Duration> getActivityDuration(
      final ActivityDirectiveId activityDirectiveId,
      final SimulationResultsInterface simulationResults
  ){
    //unfortunately results are indexed by simActId not actDirId, so have to find the one match
    return simulationResults.getSimulatedActivities().values().stream()
        .filter(simAct->simAct.directiveId().map(activityDirectiveId::equals).orElse(false))
        .collect(MoreCollectors.toOptional()) //throws if multiple
        .map(SimulatedActivity::duration);
  }

  public static ActivityDirective schedulingActToActivityDir(
      final SchedulingActivityDirective activity,
      final Map<SchedulingActivityDirectiveId, ActivityDirectiveId> planActDirectiveIdToSimulationActivityDirectiveId,
      final SchedulerModel schedulerModel) {
    if(activity.getParentActivity().isPresent()) {
      throw new Error("This method should not be called with a generated activity but with its top-level parent.");
    }
    final var arguments = new HashMap<>(activity.arguments());
    if (activity.duration() != null) {
      final var durationType = activity.getType().getDurationType();
      if (durationType instanceof DurationType.Controllable dt) {
        arguments.put(dt.parameterName(), schedulerModel.serializeDuration(activity.duration()));
      } else if (
          durationType instanceof DurationType.Uncontrollable
          || durationType instanceof DurationType.Fixed
          || durationType instanceof DurationType.Parametric
      ) {
        // If an activity has already been simulated, it will have a duration, even if its DurationType is Uncontrollable.
      } else {
        throw new Error("Unhandled variant of DurationType: " + durationType);
      }
    }
    final var serializedActivity = new SerializedActivity(activity.getType().getName(), arguments);
    return new ActivityDirective(
        activity.startOffset(),
        serializedActivity,
        planActDirectiveIdToSimulationActivityDirectiveId.get(activity.anchorId()),
        activity.anchoredToStart());
  }
}
