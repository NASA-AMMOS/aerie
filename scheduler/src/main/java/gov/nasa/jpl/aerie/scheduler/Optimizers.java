package gov.nasa.jpl.aerie.scheduler;

public class Optimizers {

  public static Optimizer earliestEndTime() {
    return new OptimizerEarliestEndTime();
  }

  public static Optimizer latestStartTime() {
    return new OptimizerLatestStartTime();
  }

}
