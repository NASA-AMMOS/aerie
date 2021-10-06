package gov.nasa.jpl.aerie.scheduler.aerie;

import gov.nasa.jpl.aerie.scheduler.ActivityType;
import gov.nasa.jpl.aerie.scheduler.Duration;

public class AerieActivityType extends ActivityType {
    public AerieActivityType(String name) {
        super(name);
    }

    public Duration duration;
}
