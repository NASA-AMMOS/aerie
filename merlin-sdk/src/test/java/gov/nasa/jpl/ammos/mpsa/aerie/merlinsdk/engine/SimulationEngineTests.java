package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import static org.junit.Assert.*;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import org.junit.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationEngineTests {

    public class DiverseStates implements StateContainer {
        public final SettableState<Double> floatState = new BasicState<>("FLOAT_STATE", 0.0);
        public final SettableState<String> stringState = new BasicState<>("STRING_STATE", "A");
        public final SettableState<List<Double>> arrayState = new BasicState<>("ARRAY_STATE", List.of(1.0, 0.0, 0.0));
        public final SettableState<Boolean> booleanState = new BasicState<>("BOOLEAN_STATE", true);

        public List<State<?>> getStateList() {
            return List.of(floatState, stringState, arrayState, booleanState);
        }
    }

    /* ------------------------ SIMULATION BASELINE TEST ------------------------ */
    @ActivityType(name="ParentActivity", states=DiverseStates.class)
    public class ParentActivity implements Activity<DiverseStates> {

        @Parameter
        public Double floatValue = 0.0;

        @Parameter
        public String stringValue = "A";

        @Parameter
        public List<Double> arrayValue = List.of(1.0, 0.0, 0.0);

        @Parameter
        public Boolean booleanValue = true;

        @Parameter
        public Duration durationValue = Duration.fromSeconds(10);

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            ChildActivity child = new ChildActivity();
            {
                child.booleanValue = this.booleanValue;
                child.durationValue = this.durationValue;
            }
            ctx.spawnActivity(child);

            SettableState<Double> floatState = states.floatState;
            Double currentFloatValue = floatState.get();
            floatState.set(floatValue);
            ctx.delay(Duration.fromSeconds(5));
            states.stringState.set(stringValue);
            states.arrayState.set(arrayValue);

        }
    }
    
    @ActivityType(name="ChildActivity", states=DiverseStates.class)
    public class ChildActivity implements Activity<DiverseStates> {

        @Parameter
        public Boolean booleanValue = true;

        @Parameter
        public Duration durationValue = Duration.fromSeconds(10);

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            ctx.delay(durationValue);
            states.booleanState.set(booleanValue);
        }
    }

    /**
     * Runs a baseline integration test with 1000 activity instances (that decompose into 1000 more)
     */
    @Test
    public void sequentialSimulationBaselineTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ParentActivity act = new ParentActivity();
            {
                act.floatValue = 1.0;
                act.stringValue = "B";
                act.arrayValue = List.of(0.0, 1.0, 0.0);
                act.booleanValue = false;
                act.durationValue = Duration.fromSeconds(10);
            }
            ActivityJob<DiverseStates> actJob = new ActivityJob<>(
                    act, simStart.add(Duration.fromHours(i))
            );
            actList.add(actJob);
        }

        DiverseStates states = new DiverseStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(states));
        engine.simulate();
    }

    /* --------------------------- TIME-ORDERING TEST --------------------------- */
    @ActivityType(name="TimeOrderingTestActivity", states=DiverseStates.class)
    public class TimeOrderingTestActivity implements Activity<DiverseStates> {

        @Parameter
        Double floatValue = 0.0;

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            states.floatState.set(floatValue);
        }
    }

    /**
     * Tests that activities are executed in order by event time and that state changes at those event times are
     * accurate.
     */
    @Test
    public void timeOrderingTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            TimeOrderingTestActivity act = new TimeOrderingTestActivity();
            {
                act.floatValue = i * 1.0;
            }
            ActivityJob<DiverseStates> actJob = new ActivityJob<>(
                    act, simStart.add(Duration.fromHours(i))
            );
            actList.add(actJob);
        }

        DiverseStates states = new DiverseStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(states));
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 1.0, floatStateHistory.get(simStart.add(Duration.fromHours(1))));
        assertEquals((Double) 2.0, floatStateHistory.get(simStart.add(Duration.fromHours(2))));
        assertEquals((Double) 3.0, floatStateHistory.get(simStart.add(Duration.fromHours(3))));
    }

    /* ------------------------------- DELAY TEST ------------------------------- */

    @ActivityType(name="DelayTestActivity", states=DiverseStates.class)
    public class DelayTestActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            states.floatState.set(1.0);
            ctx.delay(Duration.fromHours(1));
            states.floatState.set(2.0);
        }
    }

    /**
     * Tests the functionality of delays within effect models
     */
    @Test
    public void delayTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        DelayTestActivity act = new DelayTestActivity();
        Time executionTime = simStart.add(Duration.fromHours(1));
        ActivityJob<DiverseStates> actJob = new ActivityJob<>(
                act, executionTime
        );
        actList.add(actJob);

        DiverseStates states = new DiverseStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(states));
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 1.0, floatStateHistory.get(simStart.add(Duration.fromHours(1))));
        assertEquals((Double) 2.0, floatStateHistory.get(simStart.add(Duration.fromHours(2))));
    }

    /* --------------------------- SPAWN ACTIVITY TEST -------------------------- */

    @ActivityType(name="SpawnTestParentActivity", states=DiverseStates.class)
    public class SpawnTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            SpawnTestChildActivity child = new SpawnTestChildActivity();
            ctx.spawnActivity(child);
        }
    }

    @ActivityType(name="SpawnTestChildActivity", states=DiverseStates.class)
    public class SpawnTestChildActivity implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            states.floatState.set(5.0);
        }
    }

    /**
     * Tests that child activities created with `spawnActivity()` are inserted into the queue and that their effect
     * models are run at the proper simulation time.
     */
    @Test
    public void spawnActivityTimingTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        SpawnTestParentActivity parent = new SpawnTestParentActivity();
        Time parentExecutionTime = simStart.add(Duration.fromHours(1));
        ActivityJob<DiverseStates> parentJob = new ActivityJob<>(
                parent, parentExecutionTime
        );
        actList.add(parentJob);

        DiverseStates states = new DiverseStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(states));
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 5.0, floatStateHistory.get(parentExecutionTime));
    }

    /* --------------------------- CALL ACTIVITY TEST --------------------------- */

    @ActivityType(name="CallTestParentActivity", states=DiverseStates.class)
    public class CallTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            CallTestChildActivity child = new CallTestChildActivity();
            ctx.callActivity(child);
            states.floatState.set(5.0);
        }
    }

    @ActivityType(name="CallTestChildActivity", states=DiverseStates.class)
    public class CallTestChildActivity implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            ctx.delay(Duration.fromHours(2));
        }
    }

    /**
     * Tests that child activities created with `callActivity()` block the parent until effect model completion.
     */
    @Test
    public void callActivityTimingTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        CallTestParentActivity parent = new CallTestParentActivity();
        Time parentExecutionTime = simStart.add(Duration.fromHours(1));
        ActivityJob<DiverseStates> parentJob = new ActivityJob<>(
                parent, parentExecutionTime
        );
        actList.add(parentJob);

        DiverseStates states = new DiverseStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(states));
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();
        Time queryTime = parentExecutionTime.add(Duration.fromHours(2));

        assertEquals((Double) 5.0, floatStateHistory.get(queryTime));
    }

    /* -------------------------- SIMPLE DURATION TEST -------------------------- */

    @ActivityType(name="SimpleDurationTestActivity", states=DiverseStates.class)
    public class SimpleDurationTestActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            ctx.delay(Duration.fromSeconds(10));
        }
    }

    /**
     * Tests that the duration of an activity is the length in simulation time of its effect model. This test does not
     * test this condition is true for decompositions. See `parentChildDurationTest()` for that test.
     */
    @Test
    public void simpleDurationTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        SimpleDurationTestActivity act = new SimpleDurationTestActivity();
        Time executionTime = simStart.add(Duration.fromHours(1));
        ActivityJob<DiverseStates> actJob = new ActivityJob<>(
                act, executionTime
        );
        actList.add(actJob);

        DiverseStates states = new DiverseStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(states));
        engine.simulate();

        assertEquals(Duration.fromSeconds(10), engine.getActivityDuration(act));
    }

    /* ----------------------- PARENT-CHILD DURATION TEST ----------------------- */

    @ActivityType(name="DurationTestParentActivity", states=DiverseStates.class)
    public class DurationTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            DurationTestChildActivity1 child1 = new DurationTestChildActivity1();
            DurationTestChildActivity2 child2 = new DurationTestChildActivity2();
            ctx.spawnActivity(child1);
            ctx.spawnActivity(child2);
        }
    }

    @ActivityType(name="DurationTestChildActivity1", states=DiverseStates.class)
    public class DurationTestChildActivity1 implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            ctx.delay(Duration.fromSeconds(10));
        }
    }

    @ActivityType(name="DurationTestChildActivity2", states=DiverseStates.class)
    public class DurationTestChildActivity2 implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            ctx.delay(Duration.fromSeconds(5));
        }
    }

    /**
     * Tests that the duration of a parent activity matches the effect models of its children
     */
    @Test
    public void parentChildDurationTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        DurationTestParentActivity parent = new DurationTestParentActivity();
        ActivityJob<DiverseStates> parentJob = new ActivityJob<>(
                parent, simStart.add(Duration.fromHours(1))
        );
        actList.add(parentJob);

        DiverseStates states = new DiverseStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(states));
        engine.simulate();

        assertEquals(Duration.fromSeconds(10), engine.getActivityDuration(parent));
    }

    /* ----------------------- MULTI-STATE-CONTAINER TEST ----------------------- */
    
    public class SimpleStates implements StateContainer {
        public final SettableState<Integer> intState = new BasicState<>("INT_STATE", 0);

        public List<State<?>> getStateList() {
            return List.of(intState);
        }
    }

    @ActivityType(name="MultiTestActivity1", states=DiverseStates.class)
    public class MultiTestActivity1 implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext ctx, DiverseStates states) {
            states.floatState.set(5.0);
        }
    }

    @ActivityType(name="MultiTestActivity2", states=SimpleStates.class)
    public class MultiTestActivity2 implements Activity<SimpleStates> {
        @Override
        public void modelEffects(SimulationContext ctx, SimpleStates states) {
            states.intState.set(2);
        }
    }

    @Test
    /**
     * Tests that a simulation will work with activities that use multiple state containers
     */
    public void multiStateContainerTest() {
        Time simStart = new Time();

        List<ActivityJob<?>> actList = new ArrayList<>();
        MultiTestActivity1 activityOne = new MultiTestActivity1();
        MultiTestActivity2 activityTwo = new MultiTestActivity2();
        
        ActivityJob<DiverseStates> jobOne = new ActivityJob<>(
            activityOne, simStart.add(Duration.fromHours(1))
        );
        actList.add(jobOne);

        ActivityJob<SimpleStates> jobTwo = new ActivityJob<>(
            activityTwo, simStart.add(Duration.fromHours(2))
        );
        actList.add(jobTwo);

        DiverseStates diverseStates = new DiverseStates();
        SimpleStates simpleStates = new SimpleStates();

        SimulationEngine engine = new SimulationEngine(simStart, actList, List.of(simpleStates, diverseStates));
        engine.simulate();

        Map<Time, Double> floatStateHistory = diverseStates.floatState.getHistory();
        Map<Time, Integer> intStateHistory = simpleStates.intState.getHistory();

        assertEquals((Double) 5.0, floatStateHistory.get(simStart.add(Duration.fromHours(1))));
        assertEquals((Integer) 2, intStateHistory.get(simStart.add(Duration.fromHours(2))));
    }
}
