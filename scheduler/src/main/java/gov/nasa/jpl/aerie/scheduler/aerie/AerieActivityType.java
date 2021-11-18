package gov.nasa.jpl.aerie.scheduler.aerie;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.ActivityType;

public class AerieActivityType extends ActivityType {
    public AerieActivityType(String name) {
        super(name);
    }

    public Duration duration;
}
