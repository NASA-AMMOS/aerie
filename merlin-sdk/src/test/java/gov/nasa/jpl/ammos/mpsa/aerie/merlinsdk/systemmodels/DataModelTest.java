package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class DataModelTest {


    /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
    MissionModelGlue glue = new MissionModelGlue();
    Instant simStartTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

    DataSystemModel dataSystemModel;
    SettableState<Double> dataRate;
    SettableState<Double> dataVolume;
    SettableState<String> dataProtocol;

    /*----------------------------- SAMPLE SIM ---------------------------------------*/
    Instant event1 = simStartTime.plus(10, TimeUnit.SECONDS);
    Instant event2 = event1.plus(10, TimeUnit.SECONDS);
    Instant event3 = event2.plus(20, TimeUnit.SECONDS);
    Instant event4 = event3.plus(1, TimeUnit.SECONDS);
    Instant event5 = event4.plus(5, TimeUnit.SECONDS);

    @Before
    public void initialize(){
        /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
        dataSystemModel = new DataSystemModel(glue, simStartTime);
        glue.createMasterSystemModel(simStartTime, dataSystemModel);

        dataRate = new SettableState<>(GlobalPronouns.dataRate, dataSystemModel);
        dataVolume = new SettableState<>(GlobalPronouns.dataVolume, dataSystemModel);
        dataProtocol = new SettableState<>(GlobalPronouns.dataProtocol, dataSystemModel);
    }

    @Test
    public void dataVolumeTest() {
        MissionModelGlue glue = new MissionModelGlue();
        Instant simStartTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

        DataSystemModel dataSystemModel;
        SettableState<Double> dataRate;
        SettableState<Double> dataVolume;
        SettableState<String> dataProtocol;

        /*----------------------------- SAMPLE SIM ---------------------------------------*/
        Instant event1 = simStartTime.plus(10, TimeUnit.SECONDS);
        Instant event2 = event1.plus(10, TimeUnit.SECONDS);
        Instant event3 = event2.plus(20, TimeUnit.SECONDS);
        Instant event4 = event3.plus(1, TimeUnit.SECONDS);
        Instant event5 = event4.plus(5, TimeUnit.SECONDS);

        dataSystemModel = new DataSystemModel(glue, simStartTime);
        glue.createMasterSystemModel(simStartTime, dataSystemModel);

        dataRate = new SettableState<>(GlobalPronouns.dataRate, dataSystemModel);
        dataVolume = new SettableState<>(GlobalPronouns.dataVolume, dataSystemModel);
        dataProtocol = new SettableState<>(GlobalPronouns.dataProtocol, dataSystemModel);



        dataRate.set(1.0, event1);
        dataRate.set(10.0, event2);
        dataRate.set(15.0, event3);

        assertTrue(dataVolume.get()==210);
        assertTrue(dataVolume.get()==210);

        dataRate.set(0.0, event4);
        assertTrue(dataVolume.get()==225);
        assertTrue(dataVolume.get()==225);

        dataRate.set(10.0, event5);
        assertTrue(dataVolume.get()==225);
        assertTrue(dataVolume.get()==225);
    }

    @Test
    public void testSamplingGetter(){
        boolean dataRateFound = false;
        boolean dataVolumeFound = false;
        boolean dataProtocolFound = false;

        Map<String,Getter<?>> stateGetters = glue.registry().getStateGetters();


        dataRate.set(12.2, event1);
        dataProtocol.set(GlobalPronouns.spacewire, event2);
        dataVolume.set(14.4, event3);

        assertTrue(dataRate.get()==12.2);
        assertTrue(dataProtocol.get().equals(GlobalPronouns.spacewire));
        assertTrue(dataVolume.get()==14.4);

        for (var x : stateGetters.entrySet()){

            Getter<?> getter = x.getValue();
            String name = x.getKey();
            Slice slice = dataSystemModel.getInitialSlice();

            if (name.equals(GlobalPronouns.dataRate)){
                dataRateFound = true;

                var setter = glue.registry().getSetter(dataSystemModel, name);
                setter.accept(slice, 12.2);

                assertTrue(getter.klass.equals(Double.class));
                var value = (Double) getter.apply(slice);
                assertTrue(value == 12.2);

            }
            if (name.equals(GlobalPronouns.dataVolume)){
                dataVolumeFound = true;

                var setter = glue.registry().getSetter(dataSystemModel, name);
                setter.accept(slice, 33.3);

                assertTrue(getter.klass.equals(Double.class));
                var value = (Double) getter.apply(slice);
                assertTrue(value == 33.3);
            }
            if (name.equals(GlobalPronouns.dataProtocol)){
                dataProtocolFound = true;

                var setter = glue.registry().getSetter(dataSystemModel, name);
                setter.accept(slice, GlobalPronouns.spacewire);

                assertTrue(getter.klass.equals(String.class));
                var value = (String) getter.apply(slice);
                assertTrue(value.equals(GlobalPronouns.spacewire));
            }
        }
        assertTrue(dataRateFound);
        assertTrue(dataVolumeFound);
        assertTrue(dataProtocolFound);
    }
}
