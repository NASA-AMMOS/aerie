package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;

import java.util.List;

public class DemoPowerModel {

    //demo scenario configuration parameters
    public final String startTime_utcStr = "2020-001T00:00:00";
    public final String endTime_utcStr = "2021-001T00:00:00";
    public final double startSolarDistance_m = 1.496e+11; //1AU at earth
    public final double endSolarDistance_m = 7.785e+11; //5.2AU at jupiter
    public final double startBatteryCapacity_J = 10.80e+6; //150 amp hour at 20V = 10.8MJ
    public final double endBatteryCapacity_J = 0.936e+6; //13 amp hour at 20V = 0.936MJ
    public final double startBatteryCharge_pct = 70.0;
    public final double referenceSolarPower_W = 1000.0; //1KW rated solar array
    public final double instrumentAPower_W = 800.0;
    public final double instrumentBPower_W = 500.0;

    //derived configuration parameters
    public final Time startTime = new Time(); { startTime.valueOf(startTime_utcStr); }
    public final Time endTime = new Time(); { endTime.valueOf(endTime_utcStr); }

    /**
     * container type of all states used in the demonstration, pre-wired appropriately
     */
    public class States {

        /**
         * calculated state that tracks the spacecraft distance from the sun
         *
         * measured in meters
         *
         * activities may not modify this state directly
         *
         * the spacecraft solar distance is currently just a linearly increasing function
         * of time (it disregards the actual trajectory information and spice calls)
         */
        public final RandomAccessState<Double> solarDistanceState_m = new LinearInterpolatedState(
                startTime, startSolarDistance_m, endTime, endSolarDistance_m);

        /**
         * calculated state that tracks the maximum battery energy storage capacity
         *
         * measured in Joules
         *
         * activities may not modify this state directly
         *
         * the maximum battery capacity is currently just a linearly decreasing function
         * (as an approximation of fade due to charge cycles or environmental exposure)
         */
        public final RandomAccessState<Double> batteryCapacityState_J = new LinearInterpolatedState(
                startTime, startBatteryCapacity_J, endTime, endBatteryCapacity_J);

        /**
         * calculated state that tracks the power generation by the solar array panels
         *
         * measured in Watts
         *
         * activities may not modify this state directly
         *
         * the solar array power is currently calculated as an 1/r^2 decrease based on the
         * spacecraft solar distance and neglects details such as panel degredation,
         * eclipses, array tracking modes, self-shading, etc
         */
        public final SolarArrayPower solarPowerState_W = new SolarArrayPower(
                solarDistanceState_m, referenceSolarPower_W, startSolarDistance_m );

        /**
         * settable state that tracks the power consumption of the instrument
         *
         * measured in Watts
         *
         * instrument control activities may set this state directly
         *
         * the instrument power consumption is accounted for in the downstream net power
         * rollup and battery state of charge states
         */
        public final InstrumentPower instrumentAPowerState_W = new InstrumentPower();
        public final InstrumentPower instrumentBPowerState_W = new InstrumentPower();

        /**
         * calculated state that tracks the net power on the spacecraft bus
         *
         * measured in Watts
         *
         * activities may not modify this state directly (instead, turn on or off one of
         * the constituent loads)
         *
         * the net power is calculated from all the power generation sources (solar arrays)
         * minus all the power loads (subsystems, instruments, heaters, etc)
         */
        public final NetBusPower netPowerState_W = new NetBusPower(
                List.of(solarPowerState_W),
                List.of(instrumentAPowerState_W, instrumentBPowerState_W));

        /**
         * calculated state that tracks the energy stored in the battery
         *
         * measured in Joules
         *
         * activities may not modify this state directly (instead, turn on or off one of the
         * power sources or loads)
         *
         * the battery energy is calculated as the integral of the net power on the bus
         * starting from some initial charge, but clamped at the maximum capacity of
         * the battery (itself a time-varying quantity)
         */
        public final BatteryEnergy batteryEnergyState_J = new BatteryEnergy(
                startBatteryCharge_pct / 100.0 * startBatteryCapacity_J,
                netPowerState_W, batteryCapacityState_J);

        /**
         * calculated percentage charge of the battery
         *
         * measured in percentage of then-current maximum battery capacity
         *
         * activities may not modify this state directly
         *
         * the battery percentage charge is calculated as 100x the ratio of current energy
         * stored to maximum energy storage capacity at the same instant
         */
        public final BatteryPercentCharge batterStateOfChargeState_pct = new BatteryPercentCharge(
                batteryEnergyState_J, batteryCapacityState_J);
        //now hear the word of the Lord
    }//States

    /**
     * initialized and wired-together states for the demo
     */
    public States states = new States();
}
