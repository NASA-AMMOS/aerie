package gov.nasa.jpl.schedule;

import gov.nasa.jpl.schedule.models.Plan;
import gov.nasa.jpl.schedule.models.Schedule;

public class ScheduleContext {

    private Strategy strategy;
    private String planId;

    public ScheduleContext(Strategy strategy, String planId) {

        this.strategy = strategy;
        this.planId = planId;
    }

    public Schedule execute(){
        Plan plan = this.getPlan(this.planId);
        return this.strategy.execute(plan);
    }

    public Plan getPlan(String planId) {
        // call the Plan service (Camel route)
        return null;
    }

}
