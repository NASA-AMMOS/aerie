package gov.nasa.jpl.schedule;

import gov.nasa.jpl.schedule.models.Plan;
import gov.nasa.jpl.schedule.models.Schedule;

public interface Strategy {

    public Schedule execute(Plan plan);
}
