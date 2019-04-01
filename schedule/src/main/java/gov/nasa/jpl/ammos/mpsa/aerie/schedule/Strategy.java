package gov.nasa.jpl.ammos.mpsa.aerie.schedule;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Schedule;

public interface Strategy {
    public Schedule execute(Plan plan);
}
