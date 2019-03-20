package gov.nasa.jpl.schedule;

import gov.nasa.jpl.aerie.schemas.Plan;
import gov.nasa.jpl.aerie.schemas.Schedule;

public interface Strategy {
    public Schedule execute(Plan plan);
}
