package gov.nasa.jpl.aerie.scheduler.newsolver.solver.scheduler.optimizers;

public class Optimizers {

  public static Optimizer earliestEndTime() {
    return new OptimizerEarliestEndTime();
  }

  public static Optimizer latestStartTime() {
    return new OptimizerLatestStartTime();
  }

}
