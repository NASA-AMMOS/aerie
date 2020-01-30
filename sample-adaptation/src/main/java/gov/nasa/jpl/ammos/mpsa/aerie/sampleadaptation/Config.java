package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;

public class Config {

    // Aug. 1, 2016 - Aug. 1, 2018
    public final String missionStartTime_utcStr = "2016-214T00:00:00.0";
    public final String missionEndTime_utcStr = "2018-213T00:00:00.0";
    public final Time missionStartTime = Time.fromTimezoneString(missionStartTime_utcStr, "UTC");
    public final Time missionEndTime = Time.fromTimezoneString(missionEndTime_utcStr, "UTC");
    public final double startSolarDistance_m = 1.496e+11; //1AU at earth
    public final double endSolarDistance_m = 7.785e+11; //5.2AU at jupiter
    public final double startBatteryCapacity_J = 10.80e+6; //150 amp hour at 20V = 10.8MJ
    public final double startBatteryCharge_pct = 70.0;
    public final double referenceSolarPower_W = 1000.0; //1KW @1AU rated solar array
    public final double instrumentPower_W = 500.0;

}