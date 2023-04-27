package gov.nasa.jpl.aerie.scheduler.solver.optimizers;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import java.util.List;

public class OptimizerLatestStartTime extends Optimizer {

  Duration currentLatestStartTime = null;

  @Override
  public boolean isBetterThanCurrent(List<SchedulingActivityDirective> candidateGoalSolution) {
    SchedulingActivityDirective act =
        SchedulingActivityDirective.getActWithLatestStartTime(candidateGoalSolution);
    if (act == null || act.getEndTime() == null) {
      throw new IllegalStateException("Cannot optimize on uninstantiated activities");
    }
    if (currentLatestStartTime == null || act.startOffset().longerThan(currentLatestStartTime)) {
      currentGoalSolution = candidateGoalSolution;
      currentLatestStartTime = act.startOffset();
      return true;
    }
    return false;
  }
}
