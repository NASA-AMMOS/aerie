package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

@ActivityType(name="RunHeaters", generateMapper = true)
public class RunHeatersActivity implements Activity {

    @Parameter
    public double heatDuration;

    @Parameter
    public int heatIntensity;

    @Override
    public void modelEffects() {  }
}
