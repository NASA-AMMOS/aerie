package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Operator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class DataModelTest {


    /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
    MissionModelGlue glue = new MissionModelGlue();
    Instant simStartTime = SimulationInstant.ORIGIN;

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
        Instant simStartTime = SimulationInstant.ORIGIN;

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
        final var getters = glue.registry().getStateGetters();
        assertThat(getters).containsOnlyKeys("data rate", "data volume", "data protocol");

        for (var x : getters.entrySet()){
            final String name = x.getKey();
            final Getter<?> getter = x.getValue();

            final var slice = dataSystemModel.getInitialSlice();

            if (name.equals(GlobalPronouns.dataRate)){
                assertThat((Double) getter.apply(slice)).isNotEqualTo(12.2);

                var setter = glue.registry().getSetter(dataSystemModel, name);
                setter.accept(slice, 12.2);

                assertThat(getter.klass).isEqualTo(Double.class);
                assertThat((Double) getter.apply(slice)).isEqualTo(12.2);

            } else if (name.equals(GlobalPronouns.dataProtocol)){
                assertThat((String) getter.apply(slice)).isNotEqualTo(GlobalPronouns.spacewire);

                var setter = glue.registry().getSetter(dataSystemModel, name);
                setter.accept(slice, GlobalPronouns.spacewire);

                assertThat(getter.klass).isEqualTo(String.class);
                assertThat((String) getter.apply(slice)).isEqualTo(GlobalPronouns.spacewire);
            }
        }
    }


    @Test
    public void testUnion(){

        Instant event1 = simStartTime.plus(10, TimeUnit.SECONDS);
        Instant event2 = event1.plus(10, TimeUnit.SECONDS);
        Instant event3 = event2.plus(20, TimeUnit.SECONDS);
        Instant event4 = event3.plus(1, TimeUnit.SECONDS);
        Instant event5 = event4.plus(5, TimeUnit.SECONDS);

        List<Window> a = new ArrayList<>();
        List<Window> b = new ArrayList<>();

        Instant win1S = SimulationInstant.ORIGIN;
        Instant win1E = win1S.plus(5, TimeUnit.SECONDS);
        Window win1 = Window.between(win1S, win1E);

        Instant win2S = SimulationInstant.ORIGIN;
        Instant win2E = win1S.plus(9, TimeUnit.SECONDS);
        Window win2 = Window.between(win2S, win2E);

        Instant win3S = win1S.plus(8, TimeUnit.SECONDS);
        Instant win3E = win3S.plus(5, TimeUnit.SECONDS);
        Window win3 = Window.between(win3S, win3E);

        Instant win4S = win1S.plus(15, TimeUnit.SECONDS);
        Instant win4E = win4S.plus(5, TimeUnit.SECONDS);
        Window win4 = Window.between(win4S, win4E);

        Instant win5S = win1S.plus(16, TimeUnit.SECONDS);
        Instant win5E = win5S.plus(3, TimeUnit.SECONDS);
        Window win5 = Window.between(win5S, win5E);

        /*
        a: [0,5] [8,13] [16,19]
        b: [0,9] [15,20]
        u: [0,13] [15,20]
        i: [0,5] [8,9] [16,19]
         */
        a.add(win1);
        a.add(win3);
        a.add(win5);
        b.add(win2);
        b.add(win4);

        List<Window> union = Operator.union(a, b);
        System.out.println("\nUNION: ");
        union.forEach(System.out::println);

        List<Window> intersection = Operator.intersection(a, b);
        System.out.println("\nINTERSECTION: ");
        intersection.forEach(System.out::println);
    }

    @Test
    public void constraintTest(){
        /*
         * Constraint I want to build:
         * In violation if dataRate > 10 AND dataVolume > 15 AND protocol == spacewire
         */
        final var dataModel = new DataSystemModel(glue, simStartTime);
        final var dataSlice = dataModel.getInitialSlice();

        dataModel.setDataRate(dataSlice, 5.0);
        dataModel.step(dataSlice, Duration.ZERO);
        dataModel.setDataProtocol(dataSlice, GlobalPronouns.spacewire);
        dataModel.setDataRate(dataSlice, 15.0);
        dataModel.step(dataSlice, Duration.ZERO);
        dataModel.setDataProtocol(dataSlice, GlobalPronouns.UART);

        Constraint dataRateMax = () -> dataSystemModel.whenDataRateGreaterThan(dataSlice, 10.0);
        Constraint dataVolumeMax = () -> dataSystemModel.whenDataVolumeGreaterThan(dataSlice, 15.0);
        Constraint dataProtocolType = () -> dataSystemModel.whenDataProtocolEquals(dataSlice, GlobalPronouns.spacewire);
        Constraint dataSysModelConstraint = dataRateMax.and(dataVolumeMax).and(dataProtocolType);

        System.out.println("rate > 10: " + dataRateMax.getWindows());
        System.out.println("volume > 15: " + dataVolumeMax.getWindows());
        System.out.println("protocol = spacewire: " + dataProtocolType.getWindows());
        System.out.println(dataSysModelConstraint.getWindows());
    }

}
