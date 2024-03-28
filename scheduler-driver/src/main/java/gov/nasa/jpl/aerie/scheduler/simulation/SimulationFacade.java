package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.CheckpointSimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsComputerInputs;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import org.apache.commons.collections4.BidiMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public interface SimulationFacade {
  void setInitialSimResults(SimulationData simulationData);

  Duration totalSimulationTime();

  Supplier<Boolean> getCanceledListener();

  void addActivityTypes(Collection<ActivityType> activityTypes);

  SimulationResultsComputerInputs simulateNoResultsUntilEndPlan(Plan plan)
  throws SimulationException, SchedulingInterruptedException;

  SimulationResultsComputerInputs simulateNoResultsUntilEndAct(
      Plan plan,
      SchedulingActivityDirective activity) throws SimulationException, SchedulingInterruptedException;

  AugmentedSimulationResultsComputerInputs simulateNoResults(
      Plan plan,
      Duration until) throws SimulationException, SchedulingInterruptedException;

  SimulationData simulateWithResults(
      Plan plan,
      Duration until) throws SimulationException, SchedulingInterruptedException;

  SimulationData simulateWithResults(
      Plan plan,
      Duration until,
      Set<String> resourceNames) throws SimulationException, SchedulingInterruptedException;

  class SimulationException extends Exception {
    SimulationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  record AugmentedSimulationResultsComputerInputs(
      SimulationResultsComputerInputs simulationResultsComputerInputs,
      SimulationFacade.PlanSimCorrespondence planSimCorrespondence
  ) {}

  record PlanSimCorrespondence(
      BidiMap<SchedulingActivityDirectiveId, ActivityDirectiveId> planActDirectiveIdToSimulationActivityDirectiveId,
      Map<ActivityDirectiveId, ActivityDirective> directiveIdActivityDirectiveMap){
    @Override
    public boolean equals(Object other){
      if(other instanceof PlanSimCorrespondence planSimCorrespondenceAs){
        return directiveIdActivityDirectiveMap.size() == planSimCorrespondenceAs.directiveIdActivityDirectiveMap.size() &&
               new HashSet<>(directiveIdActivityDirectiveMap.values()).containsAll(new HashSet<>(((PlanSimCorrespondence) other).directiveIdActivityDirectiveMap.values()));
      }
      return false;
    }
  }
}
