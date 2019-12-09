package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.LinkedList;
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
    public class States implements StateContainer {

        //Merlin connected dem dry states, dem dry states
        public final RandomAccessState<Double> solarDistanceState_m = new LinearInterpolatedState(
                startTime, startSolarDistance_m, endTime, endSolarDistance_m);
        //solarDistance state connected to the solarPower state
        public final RandomAccessState<Double> batteryCapacityState_J = new LinearInterpolatedState(
                startTime, startBatteryCapacity_J, endTime, endBatteryCapacity_J);
        //batteryCapacity state connected to the solarPower state
        public final SolarArrayPower solarPowerState_W = new SolarArrayPower(
                solarDistanceState_m, referenceSolarPower_W, startSolarDistance_m );
        //solarPower state connected to the netPower state
        public final InstrumentPower instrumentAPowerState_W = new InstrumentPower();
        public final InstrumentPower instrumentBPowerState_W = new InstrumentPower();
        //instrumentPower states connected to the netPower state
        public final NetBusPower netPowerState_W = new NetBusPower(
                List.of(solarPowerState_W),
                List.of(instrumentAPowerState_W, instrumentBPowerState_W));
        //netPower state connected to batteryEnergy state
        public final BatteryEnergy batteryEnergyState_J = new BatteryEnergy(
                startBatteryCharge_pct / 100.0 * startBatteryCapacity_J,
                startTime, netPowerState_W, batteryCapacityState_J);
        //batteryEnergy state connected to the SOC state
        public final BatteryPercentCharge batterStateOfChargeState_pct = new BatteryPercentCharge(
                batteryEnergyState_J, batteryCapacityState_J);
        //now hear the word of the Lord

        @Override public List<State<?>> getStateList() {
            return List.of( solarDistanceState_m, batteryCapacityState_J, solarPowerState_W,
                    instrumentAPowerState_W, instrumentBPowerState_W,
                    batteryEnergyState_J, batterStateOfChargeState_pct );
        }

        /**
         * allows quickly resetting engines for all states
         *
         * @param engine the new engine to set for all states
         */
        public void setEngine(SimulationEngine engine) {
            getStateList().forEach(s->s.setEngine(engine));
        }

    }//States

    /**
     * initialized and wired-together states for the demo
     */
    public States states = new States();

    /**
     * initialized list of activities for demo
     */
    List<Activity<States>> activities = new LinkedList<>();
}
