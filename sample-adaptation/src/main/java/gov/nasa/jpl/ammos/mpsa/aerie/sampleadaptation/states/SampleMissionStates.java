package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import java.util.List;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.classes.CustomEnums.InstrumentMode;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.statemodels.data.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.statemodels.data.InstrumentDataRateModel;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.geometry.AtApojoveState;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.geometry.AtPerijoveState;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.instrument.InstrumentModeState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.DerivedState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.FunctionalState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

public class SampleMissionStates implements StateContainer {

    /**
     * configuration options that control the operation of the states
     *
     * provided at ctor
     *
     * TODO: should this have public accessors for activities to inspect?
     */
    private final Config config;

    /**
     * create a new set of interconnected mission states based on configuration
     *
     * @param config the configuration options to use to set up the states (note that
     *               subsequent changes to the configuration object itself may not be
     *               observed by the states)
     */
    public SampleMissionStates( Config config ) {

        //store configuration for possible future reference
        this.config = config;

        //create interim pretend solar distance that just grows from 1AU to 5AU over mission
        this.solarDistance_m = new LinearInterpolatedState(
                config.missionStartTime, config.startSolarDistance_m,
                config.missionEndTime, config.endSolarDistance_m);

        //solar power based on 1/r^2 as solar distance grows from 1AU
        this.solarPower_W = new SolarArrayPower(
                solarDistance_m, config.referenceSolarPower_W, config.startSolarDistance_m );

        //net power is sources minus sinks
        this.netBusPower_W = new NetBusPower(
                List.of( solarPower_W ),
                List.of( instrumentPower_W ) );

        //create pretend battery capacity that is just a constant
        this.batteryCapacity_J = new LinearInterpolatedState(
                config.missionStartTime, config.startBatteryCapacity_J,
                config.missionEndTime, config.startBatteryCapacity_J );

        //set up battery energy to start at designated initial charge at mission start
        this.batteryEnergy_J = new BatteryEnergy(
                config.startBatteryCharge_pct / 100.0 * config.startBatteryCapacity_J,
                config.missionStartTime, netBusPower_W, batteryCapacity_J );

        //battery state of charge from changing stored energy and capacity
        this.batterySOC_pct = new BatteryPercentCharge(
                batteryEnergy_J, batteryCapacity_J );


        this.atPerijove = new AtPerijoveState(config);
        this.atApojove = new AtApojoveState(config);

    }


    /**
     * settable state that tracks the power consumption of the instrument
     *
     * measured in Watts
     *
     * instrument control activities may set this state directly
     *
     * the instrument power consumption is accounted for in the downstream net power
     * rollup and battery state of charge states
     *
     * TODO: instrument power is probably easily calculated from the instrument mode
     */
    public final InstrumentPower instrumentPower_W = new InstrumentPower();

    /**
     * calculated state that tracks the spacecraft distance from the sun
     *
     * measured in meters
     *
     * activities may not modify this state directly
     *
     * the spacecraft solar distance is currently just a linearly increasing function
     * of time (it disregards the actual trajectory information and spice calls)
     * TODO: add dependency on trajectory and spice calculations
     */
    public final RandomAccessState<Double> solarDistance_m;

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
    public final SolarArrayPower solarPower_W;

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
    public NetBusPower netBusPower_W;

    /**
     * calculated state that tracks the maximum battery energy storage capacity
     *
     * measured in Joules
     *
     * activities may not modify this state directly
     *
     * the maximum battery capacity is currently just a constant (ie it neglects any
     * degredation of the battery due to charge cycles or environmental exposure)
     */
    public RandomAccessState<Double> batteryCapacity_J;

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
    public BatteryEnergy batteryEnergy_J;

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
    public BatteryPercentCharge batterySOC_pct;



    public DerivedState<Boolean> atPerijove;
    public DerivedState<Boolean> atApojove;
    // public State<Boolean> inEclipse;

    public InstrumentDataRateModel instrumentData = new InstrumentDataRateModel("Instrument", 0.0);
    public BinModel dataBin = new BinModel("DataBin", instrumentData);

    // showcasing two ways of writing derived states: make a class yourself or use `FunctionalState.derivedFrom()`
    // public DerivedState<InstrumentMode> instrumentMode = new InstrumentModeState(instrumentData);
    public DerivedState<InstrumentMode> instrumentMode = FunctionalState.derivedFrom(instrumentData::getMode);
    public DerivedState<Double> instrumentDataRate = FunctionalState.derivedFrom(instrumentData::getRate);



    public List<State<?>> getStateList() {
        return List.of(atPerijove, atApojove, instrumentData, dataBin,
                solarDistance_m, solarPower_W, netBusPower_W,
                batteryEnergy_J, batteryCapacity_J, batterySOC_pct,
                instrumentPower_W );
    }

    public List<BinModel> getBinModelList(){
        return List.of(dataBin);
    }
}