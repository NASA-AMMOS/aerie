package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.InstrumentModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DataModelTest {

    public class DataModelStates implements StateContainer {
        public final InstrumentModel instrument_a_data_rate = new InstrumentModel("instrument 1", 0.0);
        public final InstrumentModel instrument_b_data_rate = new InstrumentModel("instrument 2", 0.0);
        public final InstrumentModel instrument_c_data_rate = new InstrumentModel("instrument 3", 0.0);
        public final InstrumentModel instrument_d_data_rate = new InstrumentModel("instrument 4", 0.0);
        public final InstrumentModel instrument_e_data_rate = new InstrumentModel("instrument 4", 0.0);
        public final InstrumentModel instrument_f_data_rate = new InstrumentModel("instrument 4", 0.0);
        public final InstrumentModel instrument_g_data_rate = new InstrumentModel("instrument 4", 0.0);
        public final BinModel bin_1 = new BinModel("Bin 1", instrument_a_data_rate, instrument_b_data_rate);
        public final BinModel bin_2 = new BinModel("Bin 2", instrument_c_data_rate, instrument_d_data_rate);
        public final BinModel bin_3 = new BinModel("Bin 3", instrument_e_data_rate, instrument_f_data_rate, instrument_g_data_rate);

        public List<State<?>> getStateList() {
            return List.of(instrument_a_data_rate, instrument_b_data_rate, instrument_c_data_rate, instrument_d_data_rate,
                    instrument_e_data_rate, instrument_f_data_rate, instrument_g_data_rate, bin_1, bin_2, bin_3);
        }
    }


    /* --------------------------------- DATA MODEL SAMPLE ACTIVITIES ------------------------------------*/
    @ActivityType(name="InitBinDataVolumes", states=DataModelStates.class)
    public class InitBinDataVolumes implements Activity<DataModelStates>{


        @Override
        public void modelEffects(SimulationContext ctx, DataModelStates states){

            states.bin_1.initializeBinData();
            states.bin_2.initializeBinData();
            states.bin_3.initializeBinData();
        }
    }

    @ActivityType(name="TurnInstrumentAOn", states=DataModelStates.class)
    public class TurnInstrumentAOn implements Activity<DataModelStates> {

        @Override
        public void modelEffects(SimulationContext ctx, DataModelStates states){

            ctx.delay(Duration.fromHours(1));

            InstrumentModel some_a_instrument = states.instrument_a_data_rate;
            some_a_instrument.set(10.0);
        }
    }

    @ActivityType(name="DownlinkData", states=DataModelStates.class)
    public class DownlinkData implements Activity<DataModelStates>{

        @Override
        public void modelEffects(SimulationContext ctx, DataModelStates states){

            ctx.delay(Duration.fromHours(2));

            states.bin_1.downlink();

        }

    }


    /*----------------------------------------------------------------------------------------------------*/

    @Test
    public void basic_sim_test(){

        Time simStart = new Time();

        InitBinDataVolumes binDataVolumes = new InitBinDataVolumes();
        TurnInstrumentAOn instrumentAOnAct = new TurnInstrumentAOn();


        ActivityJob<DataModelStates> instrumentOn= new ActivityJob<>(instrumentAOnAct, simStart);
        ActivityJob<DataModelStates> binData = new ActivityJob<>(binDataVolumes, simStart);

        List<ActivityJob<?>> activityJobList = new ArrayList<>();

        activityJobList.add(instrumentOn);
        activityJobList.add(binData);

        DataModelStates states = new DataModelStates();

        SimulationEngine engine = new SimulationEngine(simStart, activityJobList, states);

        engine.simulate();

        System.out.println(engine.getCurrentSimulationTime());

    }

    @Test
    public void bin_initialization() {

        System.out.println("\nBin Initialization test start");

        Time simStart = new Time();

        InitBinDataVolumes binDataVolumes = new InitBinDataVolumes();
        ActivityJob<DataModelStates> binDataInit = new ActivityJob<>(binDataVolumes, simStart);

        List<ActivityJob<?>> activityJobList = new ArrayList<>();
        activityJobList.add(binDataInit);


        DataModelStates states = new DataModelStates();

        SimulationEngine engine = new SimulationEngine(simStart, activityJobList, states);
        engine.simulate();

        states.bin_1.printHistory();
        states.bin_2.printHistory();
        states.bin_3.printHistory();
        System.out.println("Bin Initialization test end\n");
        //add assert statements*/

        /*--------------------------------*/



    }

    @Test
    public void turn_instrument_on(){

        System.out.println("\nTurn instrument on test start");

        Time simStart = new Time();

        InitBinDataVolumes binDataVolumes = new InitBinDataVolumes();
        ActivityJob<DataModelStates> binDataInit = new ActivityJob<>(binDataVolumes, simStart);

        List<ActivityJob<?>> activityJobList = new ArrayList<>();
        activityJobList.add(binDataInit);


        DataModelStates states = new DataModelStates();

        TurnInstrumentAOn instrumentAOnAct = new TurnInstrumentAOn();
        ActivityJob<DataModelStates> instrumentOn= new ActivityJob<>(instrumentAOnAct, simStart);
        activityJobList.add(instrumentOn);

        SimulationEngine  engine = new SimulationEngine(simStart, activityJobList, states);
        engine.simulate();

        states.bin_1.printHistory();
        states.bin_2.printHistory();
        states.bin_3.printHistory();

        states.instrument_a_data_rate.printHistory();

        System.out.println("Turn instrument on test end\n");
    }

    @Test
    public void downlink_data(){

        System.out.println("\nTurn instrument on test start");

        Time simStart = new Time();

        //Create activities
        InitBinDataVolumes binDataVolumes = new InitBinDataVolumes();
        ActivityJob<DataModelStates> binDataInit = new ActivityJob<>(binDataVolumes, simStart);

        TurnInstrumentAOn instrumentAOnAct = new TurnInstrumentAOn();
        ActivityJob<DataModelStates> instrumentOn= new ActivityJob<>(instrumentAOnAct, simStart);

        DownlinkData downlinkData = new DownlinkData();
        ActivityJob<DataModelStates> downlinkAct = new ActivityJob<>(downlinkData, simStart);


        List<ActivityJob<?>> activityJobList = new ArrayList<>();
        activityJobList.add(binDataInit);
        activityJobList.add(instrumentOn);
        activityJobList.add(downlinkAct);

        DataModelStates states = new DataModelStates();

        SimulationEngine  engine = new SimulationEngine(simStart, activityJobList, states);
        engine.simulate();

        states.bin_1.printHistory();
        states.bin_2.printHistory();
        states.bin_3.printHistory();

        states.instrument_a_data_rate.printHistory();

        System.out.println("Turn instrument on test end\n");

    }
}
