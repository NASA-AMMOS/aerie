package gov.nasa.jpl.aerie.scheduler.solver.optimizers;

import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;

import java.util.List;

public abstract class Optimizer {

  List<SchedulingActivityDirective> currentGoalSolution = null;


  //incremental call
  public abstract boolean isBetterThanCurrent(List<SchedulingActivityDirective> candidateGoalSolution);

}
