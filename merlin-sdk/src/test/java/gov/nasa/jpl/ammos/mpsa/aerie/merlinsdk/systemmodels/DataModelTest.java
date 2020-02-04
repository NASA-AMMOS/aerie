package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataModelTest {
    Registry registry = new Registry();
    Instant simStartTime = registry.getStartTime();

    /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
    {
        registry.registerModel(new DataSystemModel(), new DataSystemModel.DataModelSlice(simStartTime), (registrar) -> {
            registrar.provideResource(GlobalPronouns.dataRate, Double.class, slice -> slice.getDataRate());
            registrar.provideResource(GlobalPronouns.dataVolume, Double.class, slice -> slice.getDataVolume());
            registrar.provideResource(GlobalPronouns.dataProtocol, String.class, slice -> slice.getDataProtocol());
        });
    }

    CumulableState<Double, Double> dataRate = registry.getCumulable(GlobalPronouns.dataRate, Double.class, Double.class);
    SettableState<Double> dataVolume = registry.getSettable(GlobalPronouns.dataVolume, Double.class);
    SettableState<String> dataProtocol = registry.getSettable(GlobalPronouns.dataProtocol, String.class);

    /*----------------------------- SAMPLE SIM ---------------------------------------*/
    Instant event1 = simStartTime.plus(10, TimeUnit.SECONDS);
    Instant event2 = event1.plus(10, TimeUnit.SECONDS);
    Instant event3 = event2.plus(20, TimeUnit.SECONDS);
    Instant event4 = event3.plus(1, TimeUnit.SECONDS);
    Instant event5 = event4.plus(5, TimeUnit.SECONDS);

    @Test
    public void dataVolumeTest() {
        dataRate.add(1.0, event1);
        dataRate.add(9.0, event2);
        dataRate.add(5.0, event3);

        assertEquals(dataVolume.get(), 210, 0.0);
        assertEquals(dataVolume.get(), 210, 0.0);

        dataRate.add(-15.0, event4);
        assertEquals(dataVolume.get(), 225, 0.0);
        assertEquals(dataVolume.get(), 225, 0.0);

        dataRate.add(10.0, event5);
        assertEquals(dataVolume.get(), 225, 0.0);
        assertEquals(dataVolume.get(), 225, 0.0);

        System.out.println(dataRate.getHistory());
        System.out.println(dataVolume.getHistory());
        System.out.println(dataProtocol.getHistory());
    }
}
