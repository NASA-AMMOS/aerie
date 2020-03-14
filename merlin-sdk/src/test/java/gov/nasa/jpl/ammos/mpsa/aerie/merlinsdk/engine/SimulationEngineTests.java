package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.defer;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.delay;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import org.junit.Test;

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
        public Duration durationValue = Duration.fromQuantity(10, TimeUnit.SECONDS);

        @Override
        public void modelEffects(DiverseStates states) {
            ChildActivity child = new ChildActivity();
            child.booleanValue = this.booleanValue;
            child.durationValue = this.durationValue;
            spawn(child);

            SettableState<Double> floatState = states.floatState;
            Double currentFloatValue = floatState.get();
            floatState.set(floatValue);
            delay(5L, TimeUnit.SECONDS);
            states.stringState.set(stringValue);
            states.arrayState.set(arrayValue);

        }
    }
    
    @ActivityType(name="ChildActivity", states=DiverseStates.class)
    public class ChildActivity implements Activity<DiverseStates> {

        @Parameter
        public Boolean booleanValue = true;

        @Parameter
        public Duration durationValue = Duration.fromQuantity(10, TimeUnit.SECONDS);

        @Override
        public void modelEffects(DiverseStates states) {
            delay(durationValue);
            states.booleanState.set(booleanValue);
        }
    }

    /**
     * Runs a baseline integration test with 1000 activity instances (that decompose into 1000 more)
     */
    @Test
    public void sequentialSimulationBaselineTest() {
        final var simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        final var states = new DiverseStates();

        SimulationEngine.simulate(simStart, states, () -> {
            for (int i = 0; i < 1000; i++) {
                ParentActivity act = new ParentActivity();
                act.floatValue = 1.0;
                act.stringValue = "B";
                act.arrayValue = List.of(0.0, 1.0, 0.0);
                act.booleanValue = false;
                act.durationValue = Duration.fromQuantity(10, TimeUnit.SECONDS);

                defer(i, TimeUnit.HOURS, act);
            }
        });
    }

    /* --------------------------- TIME-ORDERING TEST --------------------------- */
    @ActivityType(name="TimeOrderingTestActivity", states=DiverseStates.class)
    public class TimeOrderingTestActivity implements Activity<DiverseStates> {

        @Parameter
        Double floatValue = 0.0;

        @Override
        public void modelEffects(DiverseStates states) {
            states.floatState.set(floatValue);
        }
    }

    /**
     * Tests that activities are executed in order by event time and that state changes at those event times are
     * accurate.
     */
    @Test
    public void timeOrderingTest() {
        final var simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        final var states = new DiverseStates();

        SimulationEngine.simulate(simStart, states, () -> {
            for (int i = 1; i <= 3; i++) {
                TimeOrderingTestActivity act = new TimeOrderingTestActivity();
                act.floatValue = i * 1.0;

                defer(i, TimeUnit.HOURS, act);
            }
        });

        Map<Instant, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 1.0, floatStateHistory.get(simStart.plus(1, TimeUnit.HOURS)));
        assertEquals((Double) 2.0, floatStateHistory.get(simStart.plus(2, TimeUnit.HOURS)));
        assertEquals((Double) 3.0, floatStateHistory.get(simStart.plus(3, TimeUnit.HOURS)));
    }

    /* ------------------------------- DELAY TEST ------------------------------- */

    @ActivityType(name="DelayTestActivity", states=DiverseStates.class)
    public class DelayTestActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(DiverseStates states) {
            states.floatState.set(1.0);
            delay(1, TimeUnit.HOURS);
            states.floatState.set(2.0);
        }
    }

    /**
     * Tests the functionality of delays within effect models
     */
    @Test
    public void delayTest() {
        final var simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        final var states = new DiverseStates();

        SimulationEngine.simulate(simStart, states, () -> {
            defer(1, TimeUnit.HOURS, new DelayTestActivity());
        });

        Map<Instant, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 1.0, floatStateHistory.get(simStart.plus(1, TimeUnit.HOURS)));
        assertEquals((Double) 2.0, floatStateHistory.get(simStart.plus(2, TimeUnit.HOURS)));
    }

    /* --------------------------- SPAWN ACTIVITY TEST -------------------------- */

    @ActivityType(name="SpawnTestParentActivity", states=DiverseStates.class)
    public class SpawnTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(DiverseStates states) {
            spawn(new SpawnTestChildActivity());
        }
    }

    @ActivityType(name="SpawnTestChildActivity", states=DiverseStates.class)
    public class SpawnTestChildActivity implements Activity<DiverseStates> {
        @Override
        public void modelEffects(DiverseStates states) {
            states.floatState.set(5.0);
        }
    }

    /**
     * Tests that child activities created with `spawnActivity()` are inserted into the queue and that their effect
     * models are run at the proper simulation time.
     */
    @Test
    public void spawnActivityTimingTest() {
        final var simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        final var states = new DiverseStates();

        final var endTime = SimulationEngine.simulate(simStart, states, () -> {
            defer(1, TimeUnit.HOURS, new SpawnTestParentActivity());
        });

        Map<Instant, Double> floatStateHistory = states.floatState.getHistory();

        assertEquals((Double) 5.0, floatStateHistory.get(endTime));
    }

    /* --------------------------- CALL ACTIVITY TEST --------------------------- */

    @ActivityType(name="CallTestParentActivity", states=DiverseStates.class)
    public class CallTestParentActivity implements Activity<DiverseStates> {

        @Override
        public void modelEffects(DiverseStates states) {
            spawn(new CallTestChildActivity()).await();
            states.floatState.set(5.0);
        }
    }

    @ActivityType(name="CallTestChildActivity", states=DiverseStates.class)
    public class CallTestChildActivity implements Activity<DiverseStates> {
        @Override
        public void modelEffects(DiverseStates states) {
            delay(2, TimeUnit.HOURS);
        }
    }

    /**
     * Tests that child activities created with `callActivity()` block the parent until effect model completion.
     */
    @Test
    public void callActivityTimingTest() {
        final var simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        final var states = new DiverseStates();

        final var endTime = SimulationEngine.simulate(simStart, states, () -> {
            defer(1, TimeUnit.HOURS, new CallTestParentActivity());
        });

        Map<Instant, Double> floatStateHistory = states.floatState.getHistory();
        assertEquals((Double) 5.0, floatStateHistory.get(endTime));
    }
}
