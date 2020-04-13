package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ActivityQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintJudgement;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Operator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events.EventLog;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class DataModelTest {

    /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/
    MissionModelGlue glue = new MissionModelGlue();
    Instant simStartTime = SimulationInstant.ORIGIN;
    EventLog eventLog = new EventLog();

    DataSystemModel dataSystemModel;

    public static final class DataStates {
        SettableState<Double> dataRate;
        SettableState<Double> dataVolume;
        SettableState<String> dataProtocol;
    }

    DataStates dataStates = new DataStates();

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

        dataStates.dataRate = new SettableState<>(GlobalPronouns.dataRate, dataSystemModel, eventLog);
        dataStates.dataVolume = new SettableState<>(GlobalPronouns.dataVolume, dataSystemModel, eventLog);
        dataStates.dataProtocol = new SettableState<>(GlobalPronouns.dataProtocol, dataSystemModel, eventLog);
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

        dataStates.dataRate.set(1.0, event1);
        dataStates.dataRate.set(10.0, event2);
        dataStates.dataRate.set(15.0, event3);

        assertEquals(dataStates.dataVolume.get(), Double.valueOf(210));
        assertEquals(dataStates.dataVolume.get(), Double.valueOf(210));

        dataStates.dataRate.set(0.0, event4);
        assertEquals(dataStates.dataVolume.get(), Double.valueOf(225));
        assertEquals(dataStates.dataVolume.get(), Double.valueOf(225));

        dataStates.dataRate.set(10.0, event5);
        assertEquals(dataStates.dataVolume.get(), Double.valueOf(225));
        assertEquals(dataStates.dataVolume.get(), Double.valueOf(225));
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
         *
         * dataRate > 10: [10, 23]
         * dataVolume > 15: [3, 23]
         * dataProtocol = spacewire: [20, 23];
         */
        final var dataModel = new DataSystemModel(glue, simStartTime);
        final var dataSlice = dataModel.getInitialSlice();

        Duration duration1 = Duration.of(10,TimeUnit.SECONDS);
        Duration duration2 = Duration.of(3,TimeUnit.SECONDS);

        dataModel.setDataRate(dataSlice, 5.0);
        dataModel.step(dataSlice, duration1);

        dataModel.setDataRate(dataSlice, 15.0);
        dataModel.step(dataSlice, duration1);

        dataModel.setDataProtocol(dataSlice, GlobalPronouns.spacewire);
        dataModel.step(dataSlice, duration2);

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

    @Test
    public void temporalConstraintTest(){

        Instant time1 = SimulationInstant.ORIGIN;
        Instant time2 = time1.plus(5, TimeUnit.SECONDS);
        Instant time3 = time1.plus(30, TimeUnit.SECONDS);

        Duration duration1 = Duration.of(10,TimeUnit.SECONDS);
        Duration duration2 = Duration.of(3,TimeUnit.SECONDS);

        String activityName = "SomeDataActivity A";

        //[5,15]
        ActivityEvent<?> activityEvent1 = new ActivityEvent<>(activityName, time2, duration1);

        //[30,33]
        ActivityEvent<?> activityEvent2 = new ActivityEvent<>(activityName, time3, duration2);

        //this should be done elsewhere (sim engine?)
        eventLog.addEvent(activityEvent1);
        eventLog.addEvent(activityEvent2);

        ActivityQuerier activityQuerier = new ActivityQuerier();
        activityQuerier.provideEvents(eventLog.getAllEventsForActivity(activityName));

        assertEquals(eventLog.getAllEventsForActivity(activityName).size(), 2);

        //[5,15] , [30,33]
        //todo: add an assertEquals statement
        System.out.println("Activity occurs during these windows: " + activityQuerier.whenActivityExists());

        Constraint dataActivityOccuring = () -> activityQuerier.whenActivityExists();

        //[5,15] , [30,33]
        //todo: add an assertEquals statement
        System.out.println("\nWe can see when the activity occurs using constraints" + dataActivityOccuring.getWindows());

        //During activity the data prtoocol must be spacewire
        final var dataModel = new DataSystemModel(glue, simStartTime);
        final var dataSlice = dataModel.getInitialSlice();

        Duration duration3 = Duration.of(13,TimeUnit.SECONDS);

        //change this to 11 to see it pass
        dataModel.setDataRate(dataSlice, 10.0);
        dataModel.step(dataSlice, duration1);

        dataModel.setDataRate(dataSlice, 15.0);
        dataModel.step(dataSlice, duration1);

        dataModel.setDataProtocol(dataSlice, GlobalPronouns.spacewire);
        dataModel.step(dataSlice, duration3);

        //dataRate > 10: [10, 33]
        Constraint dataRateMax = () -> dataSystemModel.whenDataRateGreaterThan(dataSlice, 10.0);
        System.out.println(dataRateMax.getWindows());

        //[10,15], [30,33]
        Constraint rateAndActiviy = dataRateMax.and(dataActivityOccuring);
        System.out.println("Temporal and State: " + rateAndActiviy.getWindows());

        String result = ConstraintJudgement.activityDurationRequirement(dataActivityOccuring, dataRateMax);

        System.out.println(result);
    }

    @Test
    public void durationConstraintTest(){

        Instant time1 = SimulationInstant.ORIGIN;
        Instant time2 = time1.plus(5, TimeUnit.SECONDS);
        Instant time3 = time1.plus(30, TimeUnit.SECONDS);

        Duration duration1 = Duration.of(10,TimeUnit.SECONDS);
        Duration duration2 = Duration.of(3,TimeUnit.SECONDS);

        String activityName = "SomeDataActivity A";

        //[5,15]
        ActivityEvent<?> activityEvent1 = new ActivityEvent<>(activityName, time2, duration1);

        //[30,33]
        ActivityEvent<?> activityEvent2 = new ActivityEvent<>(activityName, time3, duration2);

        eventLog.addEvent(activityEvent1);
        eventLog.addEvent(activityEvent2);

        ActivityQuerier activityQuerier = new ActivityQuerier();
        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());

        Duration requiredDuration = Duration.of(10,TimeUnit.SECONDS);
        Constraint dataActivityDuration = () -> activityQuerier.whenActivityDoesNotHaveDuration(activityName, requiredDuration);

        assertEquals(dataActivityDuration.getWindows().size(), 1);
        assertEquals(dataActivityDuration.getWindows().get(0).start, time3);
        assertEquals(dataActivityDuration.getWindows().get(0).end, time3.plus(duration2));

        Duration maximumDuration = Duration.of(8, TimeUnit.SECONDS);
        Constraint dataActivityMaximumDuration = () -> activityQuerier.whenActivityHasDurationGreaterThan(activityName, maximumDuration);

        assertEquals(dataActivityMaximumDuration.getWindows().size(), 1);
        assertEquals(dataActivityMaximumDuration.getWindows().get(0).start, time2);
        assertEquals(dataActivityMaximumDuration.getWindows().get(0).end, time2.plus(duration1));

        Duration minimumDuration = Duration.of(10, TimeUnit.SECONDS);
        Constraint dataActivityMinimumDuration = () -> activityQuerier.whenActivityHasDurationLessThan(activityName, minimumDuration);

        assertEquals(dataActivityMinimumDuration.getWindows().size(), 1);
        assertEquals(dataActivityMinimumDuration.getWindows().get(0).start, time3);
        assertEquals(dataActivityMinimumDuration.getWindows().get(0).end, time3.plus(duration2));
    }

    @Test
    public void allAInstanceBeforeBInstances(){
        Instant time1 = SimulationInstant.ORIGIN;
        Instant time2 = time1.plus(5, TimeUnit.SECONDS);
        Instant time3 = time1.plus(30, TimeUnit.SECONDS);

        Duration duration1 = Duration.of(10,TimeUnit.SECONDS);
        Duration duration2 = Duration.of(3,TimeUnit.SECONDS);

        String activityNameA = "SomeDataActivity A";
        String activityNameB = "SomeDataActivity B";

        //[5,15]
        ActivityEvent<?> activityEvent1 = new ActivityEvent<>(activityNameA, time2, duration1);

        //[20,55]
        ActivityEvent<?> activityEvent2 = new ActivityEvent<>(activityNameA, time1.plus(20, TimeUnit.SECONDS), Duration.of(35, TimeUnit.SECONDS));

        //[30,33]
        ActivityEvent<?> activityEvent3 = new ActivityEvent<>(activityNameB, time3, duration2);

        eventLog.addEvent(activityEvent1);
        eventLog.addEvent(activityEvent2);
        eventLog.addEvent(activityEvent3);

        ActivityQuerier activityQuerier = new ActivityQuerier();
        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());

        Constraint whichABeforeB = () -> activityQuerier.allABeforeFirstB(activityNameA, activityNameB);
        Constraint whenAOccurring = () -> activityQuerier.whenActivityExists(activityNameA);
        Constraint checkABeforeB = whenAOccurring.minus(whichABeforeB);

        assertEquals(whichABeforeB.getWindows().size(),1);
        assertEquals(whichABeforeB.getWindows().get(0).start, time2);
        assertEquals(whichABeforeB.getWindows().get(0).end, time2.plus(duration1));

        assertEquals(checkABeforeB.getWindows().size(), 1);
    }

    @Test
    public void activityQuerierAllAEqualsB(){
        Instant time1 = SimulationInstant.ORIGIN;
        Instant time2 = time1.plus(10, TimeUnit.SECONDS);
        Instant time3 = time1.plus(20, TimeUnit.SECONDS);
        Instant time4 = time1.plus(25, TimeUnit.SECONDS);
        Instant time5 = time1.plus(40, TimeUnit.SECONDS);
        Instant time6 = time1.plus(50, TimeUnit.SECONDS);
        Instant time7 = time1.plus(51, TimeUnit.SECONDS);

        Duration duration1 = Duration.of(5,TimeUnit.SECONDS);
        Duration duration2 = Duration.of(2,TimeUnit.SECONDS);

        String activityNameA = "SomeDataActivity A";
        String activityNameB = "SomeDataActivity B";

        ActivityEvent<?> activityEvent1A = new ActivityEvent<>(activityNameA, time1, duration1);
        ActivityEvent<?> activityEvent2A = new ActivityEvent<>(activityNameA, time2, duration1);
        ActivityEvent<?> activityEvent1B = new ActivityEvent<>(activityNameB, time1, duration1);
        ActivityEvent<?> activityEvent2B = new ActivityEvent<>(activityNameB, time2, duration1);

        eventLog.addEvent(activityEvent1A);
        eventLog.addEvent(activityEvent2A);
        eventLog.addEvent(activityEvent1B);
        eventLog.addEvent(activityEvent2B);

        /**************** test case 1 ************************/
        //A: [0,5] [10,15]
        //B: [0,5] [10,15]
        //expect: [0,5], [10,15]

        ActivityQuerier activityQuerier = new ActivityQuerier();
        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());

        List<Window> allAEqualB = activityQuerier.allInstacesAAndBAreEqual(activityNameA, activityNameB);
        assertEquals(allAEqualB.size(), 2);

        List<Window> shouldContain = new ArrayList<>();
        shouldContain.add(Window.between(time1, time1.plus(duration1)));
        shouldContain.add(Window.between(time2, time2.plus(duration1)));

        assertEquals(CollectionUtils.containsAll(allAEqualB, shouldContain), true);

        /**************** test case 2 ************************/
        //A: [0,5] [10,15], [20,22], [25,27]
        //B: [0,5] [10,15], [25,27]
        //expect: [0,5], [10,15], [25,27]

        ActivityEvent<?> activityEvent3A = new ActivityEvent<>(activityNameA, time3, duration2);
        ActivityEvent<?> activityEvent4A = new ActivityEvent<>(activityNameA, time4, duration2);
        ActivityEvent<?> activityEvent3B = new ActivityEvent<>(activityNameB, time4, duration2);

        eventLog.addEvent(activityEvent3A);
        eventLog.addEvent(activityEvent4A);
        eventLog.addEvent(activityEvent3B);

        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());
        allAEqualB = activityQuerier.allInstacesAAndBAreEqual(activityNameA, activityNameB);

        shouldContain.add(Window.between(time4, time4.plus(duration2)));

        assertEquals(allAEqualB.size(), 3);
        assertEquals(CollectionUtils.containsAll(allAEqualB, shouldContain), true);

        List<Window> shouldNotContain = new ArrayList<>();
        shouldNotContain.add(Window.between(time3, time3.plus(duration2)));

        assertEquals(CollectionUtils.containsAll(allAEqualB, shouldNotContain), false);

        /**************** test case 3 ************************/
        //A: [0,5] [10,15], [20,22], [25,27], [40,45]
        //B: [0,5] [10,15], [25,27], [40,42]
        //expect: [0,5], [10,15], [25,27]

        ActivityEvent<?> activityEvent5A = new ActivityEvent<>(activityNameA, time5, duration1);
        ActivityEvent<?> activityEvent4B = new ActivityEvent<>(activityNameB, time5, duration2);

        eventLog.addEvent(activityEvent5A);
        eventLog.addEvent(activityEvent4B);

        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());
        allAEqualB = activityQuerier.allInstacesAAndBAreEqual(activityNameA, activityNameB);

        shouldNotContain = new ArrayList<>();
        shouldNotContain.add(Window.between(time5, time5.plus(duration1)));
        shouldNotContain.add(Window.between(time5, time5.plus(duration2)));

        assertEquals(CollectionUtils.containsAll(allAEqualB, shouldContain), true);
        assertEquals(CollectionUtils.containsAny(allAEqualB, shouldNotContain), false);

        /**************** test case 3 ************************/
        //A: [0,5] [10,15], [20,22], [25,27], [40,45], [50,52]
        //B: [0,5] [10,15], [25,27], [40,42], [51,56]
        //expect: [0,5], [10,15], [25,27]

        ActivityEvent<?> activityEvent6A = new ActivityEvent<>(activityNameA, time6, duration1);
        ActivityEvent<?> activityEvent5B = new ActivityEvent<>(activityNameB, time7, duration2);

        eventLog.addEvent(activityEvent6A);
        eventLog.addEvent(activityEvent5B);

        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());
        allAEqualB = activityQuerier.allInstacesAAndBAreEqual(activityNameA, activityNameB);

        shouldNotContain = new ArrayList<>();
        shouldNotContain.add(Window.between(time6, time6.plus(duration1)));
        shouldNotContain.add(Window.between(time7, time7.plus(duration2)));

        assertEquals(CollectionUtils.containsAll(allAEqualB, shouldContain), true);
        assertEquals(CollectionUtils.containsAny(allAEqualB, shouldNotContain), false);

    }

    @Test
    public void allAInstancesEqualsBInstances(){
        Instant time1 = SimulationInstant.ORIGIN;
        Instant time2 = time1.plus(10, TimeUnit.SECONDS);
        Instant time3 = time1.plus(20, TimeUnit.SECONDS);

        Duration duration1 = Duration.of(5,TimeUnit.SECONDS);
        Duration duration2 = Duration.of(2,TimeUnit.SECONDS);

        String activityNameA = "SomeDataActivity A";
        String activityNameB = "SomeDataActivity B";

        ActivityEvent<?> activityEvent1A = new ActivityEvent<>(activityNameA, time1, duration1);
        ActivityEvent<?> activityEvent2A = new ActivityEvent<>(activityNameA, time2, duration1);
        ActivityEvent<?> activityEvent1B = new ActivityEvent<>(activityNameB, time1, duration1);
        ActivityEvent<?> activityEvent2B = new ActivityEvent<>(activityNameB, time2, duration1);

        eventLog.addEvent(activityEvent1A);
        eventLog.addEvent(activityEvent2A);
        eventLog.addEvent(activityEvent1B);
        eventLog.addEvent(activityEvent2B);

        ActivityQuerier activityQuerier = new ActivityQuerier();
        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());

        Constraint whichAAndBAreEqual = () -> activityQuerier.allInstacesAAndBAreEqual(activityNameA, activityNameB);
        Constraint whenAOccurring = () -> activityQuerier.whenActivityExists(activityNameA);
        Constraint whenBOccurring = () -> activityQuerier.whenActivityExists(activityNameB);

        Constraint violatingInstancesOfAAndB = whenAOccurring.minus(whichAAndBAreEqual).or(whenBOccurring.minus(whichAAndBAreEqual));

        assertEquals(whichAAndBAreEqual.getWindows().size(), 2);
        assertEquals(violatingInstancesOfAAndB.getWindows().size(), 0);

        ActivityEvent<?> activityEvent3A = new ActivityEvent<>(activityNameA, time3, duration2);

        eventLog.addEvent(activityEvent3A);
        activityQuerier.provideActivityEventMap(eventLog.getCompleteActivityMap());

        assertEquals(violatingInstancesOfAAndB.getWindows().size(), 1);
        assertEquals(violatingInstancesOfAAndB.getWindows().get(0).start, time3);
        assertEquals(violatingInstancesOfAAndB.getWindows().get(0).end, time3.plus(duration2));
    }

}
