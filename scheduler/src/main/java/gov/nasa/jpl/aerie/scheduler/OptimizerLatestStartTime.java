package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

public class OptimizerLatestStartTime extends Optimizer{

    Time currentLatestStartTime = null;

    @Override
    public boolean isBetterThanCurrent(List<ActivityInstance> candidateGoalSolution) {
        ActivityInstance act = ActivityInstance.getActWithLatestStartTtime(candidateGoalSolution);

        if(currentLatestStartTime == null || act.getStartTime().biggerThan(currentLatestStartTime)){
            currentGoalSolution = candidateGoalSolution;
            currentLatestStartTime = act.getStartTime();
            return true;
        }
        return false;
    }

}
