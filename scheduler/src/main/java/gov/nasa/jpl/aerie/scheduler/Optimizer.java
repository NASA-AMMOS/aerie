package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

public abstract class Optimizer {

    List<ActivityInstance> currentGoalSolution=null;



    //incremental call
    public abstract boolean isBetterThanCurrent(List<ActivityInstance> candidateGoalSolution);

}
