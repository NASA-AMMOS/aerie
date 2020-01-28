package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DataModelTest {


    /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
    MissionModelGlue glue = new MissionModelGlue();
    Time simStartTime = new Time();
    DataSystemModel dataSystemModel = new DataSystemModel(glue, simStartTime);

    SettableState<Double> dataRate = new SettableState<>(GlobalPronouns.dataRate, dataSystemModel);
    SettableState<Double> dataVolume = new SettableState<>(GlobalPronouns.dataVolume, dataSystemModel);
    SettableState<String> dataProtocol = new SettableState<>(GlobalPronouns.UART, dataSystemModel);

    /*----------------------------- SAMPLE SIM ---------------------------------------*/
    Time event1 = simStartTime.add(Duration.fromSeconds(10));
    Time event2 = event1.add(Duration.fromSeconds(10));
    Time event3 = event2.add(Duration.fromSeconds(20));
    Time event4 = event3.add(Duration.fromSeconds(1));
    Time event5 = event4.add(Duration.fromSeconds(5));

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
