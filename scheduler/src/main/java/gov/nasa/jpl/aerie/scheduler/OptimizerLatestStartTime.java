package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

public class OptimizerLatestStartTime extends Optimizer {

  Duration currentLatestStartTime = null;

  @Override
  public boolean isBetterThanCurrent(List<ActivityInstance> candidateGoalSolution) {
    ActivityInstance act = ActivityInstance.getActWithLatestStartTtime(candidateGoalSolution);
    if(act == null || !act.hasEndTime()) {
      throw new IllegalStateException("Cannot optimize on uninstantiated activities");
    }
    if (currentLatestStartTime == null || act.getStartTime().longerThan(currentLatestStartTime)) {
      currentGoalSolution = candidateGoalSolution;
      currentLatestStartTime = act.getStartTime();
      return true;
    }
    return false;
  }

}
