package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.DoubleState;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.List;

public class Config {

    // Aug. 1, 2016 - Aug. 1, 2018
    public static final String missionStartTime_utcStr = "2016-214T00:00:00.0";
    public static final String missionEndTime_utcStr = "2018-213T00:00:00.0";
    public static final double startSolarDistance_m = 1.496e+11; //1AU at earth
    public static final double endSolarDistance_m = 7.785e+11; //5.2AU at jupiter
    public static final double startBatteryCapacity_J = 10.80e+6; //150 amp hour at 20V = 10.8MJ
    public static final double startBatteryCharge_pct = 70.0;
    public static final double initialBatteryCapacity = startBatteryCapacity_J*startBatteryCharge_pct/100;
    public static final double referenceSolarPower_W = 1000.0; //1KW @1AU rated solar array
    public static final double instrumentPower = 500.0; // Watts = J/s
    public static final double cameraHeaterPower = 200.0; // Watts = J/s
    public static final double cameraRotationRate = 0.0027777777777778; // radians/s (0.5 degrees per second)
    public static final double cameraRotationPower = 50.0; // Watts J/s
    public static final long downlinkRate = (long)2000.0; // bits per second

    // Ordering prioritizes which data gets downlinked first
    public static final List<DoubleState> downlinkPriority = List.of(
            SampleMissionStates.instrumentDataBits,
            SampleMissionStates.cameraDataBits
    );
}
