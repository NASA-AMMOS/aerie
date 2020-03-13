package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.InstrumentModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.delay;

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
    public static class InitBinDataVolumes implements Activity<DataModelStates>{


        @Override
        public void modelEffects(DataModelStates states){

            states.bin_1.initializeBinData();
            states.bin_2.initializeBinData();
            states.bin_3.initializeBinData();
        }
    }

    @ActivityType(name="TurnInstrumentAOn", states=DataModelStates.class)
    public static class TurnInstrumentAOn implements Activity<DataModelStates> {

        @Override
        public void modelEffects(DataModelStates states){

            delay(1, TimeUnit.HOURS);

            InstrumentModel some_a_instrument = states.instrument_a_data_rate;
            some_a_instrument.set(10.0);
        }
    }

    @ActivityType(name="DownlinkData", states=DataModelStates.class)
    public static class DownlinkData implements Activity<DataModelStates>{

        @Override
        public void modelEffects(DataModelStates states){

            delay(2, TimeUnit.HOURS);

            states.bin_1.downlink();

        }

    }


    /*----------------------------------------------------------------------------------------------------*/

    @Test
    public void basic_sim_test(){

        Instant simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

        final var activityList = List.of(
            Pair.of(simStart, new TurnInstrumentAOn()),
            Pair.of(simStart, new InitBinDataVolumes())
        );

        DataModelStates states = new DataModelStates();

        final var simEnd = SimulationEngine.simulate(simStart, activityList, states);
        System.out.println(simEnd);

    }

    @Test
    public void bin_initialization() {

        System.out.println("\nBin Initialization test start");

        Instant simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

        final List<Pair<Instant, ? extends Activity<DataModelStates>>> activityJobList = List.of(
            Pair.of(simStart, new InitBinDataVolumes())
        );

        DataModelStates states = new DataModelStates();

        SimulationEngine.simulate(simStart, activityJobList, states);

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

        Instant simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

        final List<Pair<Instant, ? extends Activity<DataModelStates>>> activityJobList = List.of(
            Pair.of(simStart, new InitBinDataVolumes()),
            Pair.of(simStart, new TurnInstrumentAOn())
        );

        DataModelStates states = new DataModelStates();

        SimulationEngine.simulate(simStart, activityJobList, states);

        states.bin_1.printHistory();
        states.bin_2.printHistory();
        states.bin_3.printHistory();

        states.instrument_a_data_rate.printHistory();

        System.out.println("Turn instrument on test end\n");
    }

    @Test
    public void downlink_data(){

        System.out.println("\nTurn instrument on test start");

        Instant simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

        //Create activities
        final List<Pair<Instant, ? extends Activity<DataModelStates>>> activityJobList = List.of(
            Pair.of(simStart, new InitBinDataVolumes()),
            Pair.of(simStart, new TurnInstrumentAOn()),
            Pair.of(simStart, new DownlinkData())
        );

        DataModelStates states = new DataModelStates();

        SimulationEngine.simulate(simStart, activityJobList, states);

        states.bin_1.printHistory();
        states.bin_2.printHistory();
        states.bin_3.printHistory();

        states.instrument_a_data_rate.printHistory();

        System.out.println("Turn instrument on test end\n");

    }
}
