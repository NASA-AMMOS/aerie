package gov.nasa.jpl.aerie.scheduler.solver.optimizers;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;

import java.util.List;

public class OptimizerLatestStartTime extends Optimizer {

  Duration currentLatestStartTime = null;

  @Override
  public boolean isBetterThanCurrent(List<ActivityInstance> candidateGoalSolution) {
    ActivityInstance act = ActivityInstance.getActWithLatestStartTtime(candidateGoalSolution);
    if(act == null || act.getEndTime() == null) {
      throw new IllegalStateException("Cannot optimize on uninstantiated activities");
    }
    if (currentLatestStartTime == null || act.startTime().longerThan(currentLatestStartTime)) {
      currentGoalSolution = candidateGoalSolution;
      currentLatestStartTime = act.startTime();
      return true;
    }
    return false;
  }

}
