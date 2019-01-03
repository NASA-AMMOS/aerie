package gov.nasa.jpl.mpsa.europa_clipper.activities.models.bodies;

import gov.nasa.jpl.mpsa.activities.ActivityType;
import gov.nasa.jpl.mpsa.activities.Parameter;
import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.time.Time;

public class Moon extends ActivityType {

    AdaptationModel EarthModel = new BodyModel();

    public void setParameters() {

        Parameter body = new Parameter.Builder("body")
                .withValue("Moon")
                .ofType(String.class)
                .build();
        Parameter start = new Parameter.Builder("StartTime")
                .withValue(new Time("2026-339T00:00:00.000"))
                .ofType(Time.class)
                .build();
        Parameter end = new Parameter.Builder("EndTime")
                .withValue(new Time("2036-339T00:00:00.000"))
                .ofType(Time.class)
                .build();
//        Parameter current_time = new Parameter.Builder("CurrentTime")
//                .withValue(new Time("2030-339T00:00:00.000"))
//                .ofType(Time.class)
//                .build();

        this.addParameter(body);
        this.addParameter(start);
        this.addParameter(end);
//        this.addParameter(current_time);

    }
}
