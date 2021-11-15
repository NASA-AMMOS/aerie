package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

public class OptimizerEarliestEndTime extends Optimizer {

  Time currentEarliestEndTime = null;

  @Override
  public boolean isBetterThanCurrent(List<ActivityInstance> candidateGoalSolution) {
    ActivityInstance act = ActivityInstance.getActWithEarliestEndTtime(candidateGoalSolution);

    if (currentEarliestEndTime == null || act.getEndTime().smallerThan(currentEarliestEndTime)) {
      currentGoalSolution = candidateGoalSolution;
      currentEarliestEndTime = act.getEndTime();
      return true;
    }
    return false;
  }


}
