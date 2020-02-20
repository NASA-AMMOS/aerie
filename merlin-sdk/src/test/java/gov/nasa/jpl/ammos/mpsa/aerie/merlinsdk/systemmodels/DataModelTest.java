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


    @Test
    public void testUnion(){

        Instant event1 = simStartTime.plus(10, TimeUnit.SECONDS);
        Instant event2 = event1.plus(10, TimeUnit.SECONDS);
        Instant event3 = event2.plus(20, TimeUnit.SECONDS);
        Instant event4 = event3.plus(1, TimeUnit.SECONDS);
        Instant event5 = event4.plus(5, TimeUnit.SECONDS);

        List<Window> a = new ArrayList<>();
        List<Window> b = new ArrayList<>();

        Instant win1S = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        Instant win1E = win1S.plus(5, TimeUnit.SECONDS);
        Window win1 = Window.between(win1S, win1E);

        Instant win2S = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
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
         * In violation if dataRate > 10 AND dataVolume > 100 AND protocol == spacewire
         */
        final var dataModel = new DataSystemModel(glue, simStartTime);
        final var dataSlice = dataModel.getInitialSlice();

        dataModel.setDataRate(dataSlice, 5.0);
        dataModel.step(dataSlice, Duration.fromQuantity(5, TimeUnit.SECONDS));
        dataModel.setDataProtocol(dataSlice, GlobalPronouns.spacewire);
        dataModel.setDataRate(dataSlice, 15.0);
        dataModel.step(dataSlice, Duration.fromQuantity(5, TimeUnit.SECONDS));
        dataModel.setDataProtocol(dataSlice, GlobalPronouns.UART);

        Constraint dataRateMax = () -> dataSystemModel.whenDataRateGreaterThan(dataSlice, 10.0);
        Constraint dataVolumeMax = () -> dataSystemModel.whenDataVolumeGreaterThan(dataSlice, 100.0);
        Constraint dataProtocolType = () -> dataSystemModel.whenDataProtocolEquals(dataSlice, GlobalPronouns.spacewire);
        Constraint ratesAndVol = Operator.And(dataRateMax, dataVolumeMax);
        Constraint dataSysModelConstraint = Operator.And(ratesAndVol, dataProtocolType);

        System.out.println(dataRateMax.getWindows());
        System.out.println(dataProtocolType.getWindows());
        System.out.println(dataSysModelConstraint.getWindows());
    }

}
