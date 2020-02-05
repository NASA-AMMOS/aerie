package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataModelTest {
    Registry registry = new Registry();
    Instant simStartTime = registry.getStartTime();

    /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
    {
        registry.registerModel(new DataSystemModel.DataModelSlice(simStartTime), registrar -> {
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
    public void dataVolumeRegistryTest() {
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
    }

    @Test
    public void dataVolumeTest() {
        final var simStartTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        final var slice = new DataSystemModel.DataModelSlice(simStartTime);

        slice.step(Duration.fromQuantity(10, TimeUnit.SECONDS));
        slice.accumulateDataRate(1.0);
        slice.step(Duration.fromQuantity(10, TimeUnit.SECONDS));
        slice.accumulateDataRate(9.0);
        slice.step(Duration.fromQuantity(20, TimeUnit.SECONDS));
        slice.accumulateDataRate(5.0);

        assertEquals(0*10 + 1*10 + 10*20, slice.getDataVolume(), 0.0);

        slice.step(Duration.fromQuantity(1, TimeUnit.SECONDS));
        slice.accumulateDataRate(-15.0);

        assertEquals(0*10 + 1*10 + 10*20 + 15*1, slice.getDataVolume(), 0.0);

        slice.step(Duration.fromQuantity(5, TimeUnit.SECONDS));
        slice.accumulateDataRate(10.0);

        assertEquals(0*10 + 1*10 + 10*20 + 15*1 + 0*5, slice.getDataVolume(), 0.0);

        System.out.println(slice.getDataRateHistory());
        System.out.println(slice.getDataProtocolHistory());
    }
}
