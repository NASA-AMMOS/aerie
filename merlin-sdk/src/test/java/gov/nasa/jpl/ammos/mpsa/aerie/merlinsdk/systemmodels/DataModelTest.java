package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DataModelTest {


    /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
    MissionModelGlue glue = new MissionModelGlue();
    Instant simStartTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
    DataSystemModel dataSystemModel = new DataSystemModel(glue, simStartTime);

    SettableState<Double> dataRate = new SettableState<>(GlobalPronouns.dataRate, dataSystemModel);
    SettableState<Double> dataVolume = new SettableState<>(GlobalPronouns.dataVolume, dataSystemModel);
    SettableState<String> dataProtocol = new SettableState<>(GlobalPronouns.UART, dataSystemModel);

    /*----------------------------- SAMPLE SIM ---------------------------------------*/
    Instant event1 = simStartTime.plus(10, TimeUnit.SECONDS);
    Instant event2 = event1.plus(10, TimeUnit.SECONDS);
    Instant event3 = event2.plus(20, TimeUnit.SECONDS);
    Instant event4 = event3.plus(1, TimeUnit.SECONDS);
    Instant event5 = event4.plus(5, TimeUnit.SECONDS);

    @Test
    public void dataVolumeTest() {
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
}
