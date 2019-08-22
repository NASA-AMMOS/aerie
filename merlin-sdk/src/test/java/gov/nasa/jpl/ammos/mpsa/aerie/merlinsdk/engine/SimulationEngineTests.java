package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationEngineTests {

    public class DiverseStates implements StateContainer {
        public final SettableState<Double> floatState = new SettableState<>();
        public final SettableState<String> stringState = new SettableState<>();
        public final SettableState<List<Double>> arrayState = new SettableState<>();
        public final SettableState<Boolean> booleanState = new SettableState<>();

        public List<SettableState<?>> getStateList() {
            return List.of(floatState, stringState, arrayState, booleanState);
        }
    }

    /* ------------------------ SIMULATION BASELINE TEST ------------------------ */

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
        public Duration durationValue = new Duration(10 * Duration.ONE_SECOND);

        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            ChildActivity child = new ChildActivity();
            {
                child.booleanValue = this.booleanValue;
                child.durationValue = this.durationValue;
            }
            ctx.spawnActivity(child);
            
            SettableState<Double> floatState = states.floatState;
            Double currentFloatValue = floatState.getValue();
            floatState.setValue(floatValue);
            ctx.delay(new Duration(5 * Duration.ONE_SECOND));
            states.stringState.setValue(stringValue);
            states.arrayState.setValue(arrayValue);

        }
    }

    public class ChildActivity implements Activity<DiverseStates> {

        @Parameter
        public Boolean booleanValue = true;

        @Parameter
        public Duration durationValue = new Duration(10 * Duration.ONE_SECOND);

        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            ctx.delay(durationValue);
            states.booleanState.setValue(booleanValue);
        }
    }

    /**
     * Runs a baseline integration test with 1000 activity instances (that decompose into 1000 more)
     */
    @Test
    public void sequentialSimulationBaselineTest() {
        Time simStart = new Time();

        List<ActivityThread<DiverseStates>> actList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ParentActivity act = new ParentActivity();
            {
                act.floatValue = 1.0;
                act.stringValue = "B";
                act.arrayValue = List.of(0.0, 1.0, 0.0);
                act.booleanValue = false;
                act.durationValue = new Duration(10 * Duration.ONE_SECOND);
            }
            ActivityThread<DiverseStates> actThread = new ActivityThread<>(
                act, simStart.add(new Duration(i * 1 * Duration.ONE_HOUR))
            );
            actList.add(actThread);
        }

        DiverseStates states = new DiverseStates();

        SimulationEngine<DiverseStates> engine = new SimulationEngine<>(simStart, actList, states);
        engine.simulate();
    }

    /* --------------------------- TIME-ORDERING TEST --------------------------- */

    public class TimeOrderingTestActivity implements Activity<DiverseStates> {

        @Parameter
        Double floatValue = 0.0;

        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            states.floatState.setValue(floatValue);
        }
    }

    /**
     * Tests that activities are executed in order by event time and that state changes at those event times are
     * accurate.
     */
    @Test
    public void timeOrderingTest() {
        Time simStart = new Time();

        List<ActivityThread<DiverseStates>> actList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
           TimeOrderingTestActivity act = new TimeOrderingTestActivity();
            {
                act.floatValue = i * 1.0;
            }
            ActivityThread<DiverseStates> actThread = new ActivityThread<>(
                act, simStart.add(new Duration(i * 1 * Duration.ONE_HOUR))
            );
            actList.add(actThread);
        }

        DiverseStates states = new DiverseStates();

        SimulationEngine<DiverseStates> engine = new SimulationEngine<>(simStart, actList, states);
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 1.0, floatStateHistory.get(simStart.add(new Duration(1 * Duration.ONE_HOUR))));
        assertEquals((Double) 2.0, floatStateHistory.get(simStart.add(new Duration(2 * Duration.ONE_HOUR))));
        assertEquals((Double) 3.0, floatStateHistory.get(simStart.add(new Duration(3 * Duration.ONE_HOUR))));
    }

    /* ------------------------------- DELAY TEST ------------------------------- */

    public class DelayTestActivity implements Activity<DiverseStates> {
        
        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            states.floatState.setValue(1.0);
            ctx.delay(new Duration(1 * Duration.ONE_HOUR));
            states.floatState.setValue(2.0);
        }
    }

    /**
     * Tests the functionality of delays within effect models
     */
    @Test
    public void delayTest() {
        Time simStart = new Time();

        List<ActivityThread<DiverseStates>> actList = new ArrayList<>();
        DelayTestActivity act = new DelayTestActivity();
        Time executionTime = simStart.add(new Duration(1 * Duration.ONE_HOUR));
        ActivityThread<DiverseStates> actThread = new ActivityThread<>(
            act, executionTime
        );
        actList.add(actThread);

        DiverseStates states = new DiverseStates();

        SimulationEngine<DiverseStates> engine = new SimulationEngine<>(simStart, actList, states);
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 1.0, floatStateHistory.get(simStart.add(new Duration(1 * Duration.ONE_HOUR))));
        assertEquals((Double) 2.0, floatStateHistory.get(simStart.add(new Duration(2 * Duration.ONE_HOUR))));
    }

    /* --------------------------- SPAWN ACTIVITY TEST -------------------------- */

    public class SpawnTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            SpawnTestChildActivity child = new SpawnTestChildActivity();
            ctx.spawnActivity(child);
        }
    }

    public class SpawnTestChildActivity implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            states.floatState.setValue(5.0);
        }
    }

    /**
     * Tests that child activities created with `spawnActivity()` are inserted into the queue and that their effect
     * models are run at the proper simulation time.
     */
    @Test
    public void spawnActivityTimingTest() {
        Time simStart = new Time();

        List<ActivityThread<DiverseStates>> actList = new ArrayList<>();
        SpawnTestParentActivity parent = new SpawnTestParentActivity();
        Time parentExecutionTime = simStart.add(new Duration(1 * Duration.ONE_HOUR));
        ActivityThread<DiverseStates> parentThread = new ActivityThread<>(
            parent, parentExecutionTime
        );
        actList.add(parentThread);

        DiverseStates states = new DiverseStates();

        SimulationEngine<DiverseStates> engine = new SimulationEngine<>(simStart, actList, states);
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 5.0, floatStateHistory.get(parentExecutionTime));
    }

    /* --------------------------- CALL ACTIVITY TEST --------------------------- */

    public class CallTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            CallTestChildActivity child = new CallTestChildActivity();
            ctx.callActivity(child);
            states.floatState.setValue(5.0);
        }
    }

    public class CallTestChildActivity implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            ctx.delay(new Duration(2 * Duration.ONE_HOUR));
        }
    }

    /**
     * Tests that child activities created with `callActivity()` block the parent until effect model completion.
     */
    @Test
    public void callActivityTimingTest() {
        Time simStart = new Time();

        List<ActivityThread<DiverseStates>> actList = new ArrayList<>();
        CallTestParentActivity parent = new CallTestParentActivity();
        Time parentExecutionTime = simStart.add(new Duration(1 * Duration.ONE_HOUR));
        ActivityThread<DiverseStates> parentThread = new ActivityThread<>(
            parent, parentExecutionTime
        );
        actList.add(parentThread);

        DiverseStates states = new DiverseStates();

        SimulationEngine<DiverseStates> engine = new SimulationEngine<>(simStart, actList, states);
        engine.simulate();

        Map<Time, Double> floatStateHistory = states.floatState.getHistory();
        Time queryTime = parentExecutionTime.add(new Duration(2 * Duration.ONE_HOUR));

        assertEquals((Double) 5.0, floatStateHistory.get(queryTime));
    }

    /* -------------------------- SIMPLE DURATION TEST -------------------------- */

    public class SimpleDurationTestActivity implements Activity<DiverseStates> {
        
        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            ctx.delay(new Duration(10 * Duration.ONE_SECOND));
        }
    }

    /**
     * Tests that the duration of an activity is the length in simulation time of its effect model. This test does not
     * test this condition is true for decompositions. See `parentChildDurationTest()` for that test.
     */
    @Test
    public void simpleDurationTest() {
        Time simStart = new Time();

        List<ActivityThread<DiverseStates>> actList = new ArrayList<>();
        SimpleDurationTestActivity act = new SimpleDurationTestActivity();
        Time executionTime = simStart.add(new Duration(1 * Duration.ONE_HOUR));
        ActivityThread<DiverseStates> actThread = new ActivityThread<>(
            act, executionTime
        );
        actList.add(actThread);

        DiverseStates states = new DiverseStates();

        SimulationEngine<DiverseStates> engine = new SimulationEngine<>(simStart, actList, states);
        engine.simulate();

        assertEquals(new Duration(10 * Duration.ONE_SECOND), engine.getActivityDuration(act));
    }

    /* ----------------------- PARENT-CHILD DURATION TEST ----------------------- */

    public class DurationTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            DurationTestChildActivity1 child1 = new DurationTestChildActivity1();
            DurationTestChildActivity2 child2 = new DurationTestChildActivity2();
            ctx.spawnActivity(child1);
            ctx.spawnActivity(child2);
        }
    }

    public class DurationTestChildActivity1 implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            ctx.delay(new Duration(10 * Duration.ONE_SECOND));
        }
    }

    public class DurationTestChildActivity2 implements Activity<DiverseStates> {
        @Override
        public void modelEffects(SimulationContext<DiverseStates> ctx, DiverseStates states) {
            ctx.delay(new Duration(5 * Duration.ONE_SECOND));
        }
    }

    /**
     * Tests that the duration of a parent activity matches the effect models of its children
     */
    @Test
    public void parentChildDurationTest() {
        Time simStart = new Time();

        List<ActivityThread<DiverseStates>> actList = new ArrayList<>();
        DurationTestParentActivity parent = new DurationTestParentActivity();
        ActivityThread<DiverseStates> parentThread = new ActivityThread<>(
            parent, simStart.add(new Duration(1 * Duration.ONE_HOUR))
        );
        actList.add(parentThread);

        DiverseStates states = new DiverseStates();

        SimulationEngine<DiverseStates> engine = new SimulationEngine<>(simStart, actList, states);
        engine.simulate();

        assertEquals(new Duration(10 * Duration.ONE_SECOND), engine.getActivityDuration(parent));
    }
}
