package gov.nasa.jpl.mpsa.adaptation.activities;

import gov.nasa.jpl.mpsa.activities.ActivityType;
import gov.nasa.jpl.mpsa.activities.Parameter;
import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.adaptation.activities.models.ExampleModel;
import gov.nasa.jpl.mpsa.time.Time;

public class ExampleActivity extends ActivityType {

    AdaptationModel myExampleModel = new ExampleModel();

    public void setParameters() {
        Parameter start = new Parameter.Builder("someParam")
                .withValue(10)
                .ofType(Integer.class)
                .build();

        this.addParameter(start);
    }

}
