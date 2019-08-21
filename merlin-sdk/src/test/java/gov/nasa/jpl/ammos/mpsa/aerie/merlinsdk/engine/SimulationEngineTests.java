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

    // public class SampleStates implements StateContainer {
    //     public final SettableState<Double> stateA = new SettableState<>();
    //     public final SettableState<String> stateB = new SettableState<>();

    //     public List<SettableState<?>> getStateList() {
    //         return List.of(stateA, stateB);
    //     }
    // }

    // public class Activity1 implements Activity<SampleStates> {

    //     @Override
    //     public void modelEffects(SimulationContext<SampleStates> ctx, SampleStates states) {
    //         System.out.println("\tActivity1 effect model - part 1!");
    //         states.stateA.setValue(1.1);
    //         ctx.delay(new Duration(4 * Duration.ONE_DAY));

    //         Activity1A child = new Activity1A();
    //         System.out.println("\tSpawning child Activity1A in parallel!");
    //         ctx.spawnActivity(child);
    //         System.out.println("\tActivity1 effect model - part 2!");
    //         states.stateA.setValue(1.2);
    //         ctx.delay(new Duration(2 * Duration.ONE_DAY));

    //         System.out.println("\tActivity1 effect model - part 3!");
    //         states.stateA.setValue(1.3);
    //         ctx.callActivity(new Activity1B());

    //         System.out.println("\tActivity1 effect model part 4 (should be 5 seconds after Activity1B first starts)");
    //         states.stateA.setValue(1.4);
    //     }

    // }

    // public class Activity2 implements Activity<SampleStates> {

    //     @Override
    //     public void modelEffects(SimulationContext<SampleStates> ctx, SampleStates states) {
    //         System.out.println("\tActivity2 effect model - part 1!");
    //         states.stateA.setValue(2.1);
    //         ctx.delay(new Duration(1 * Duration.ONE_DAY));

    //         System.out.println("\tActivity2 effect model - part 2!");
    //         states.stateA.setValue(2.2);
    //     }
        
    // }

    // public class Activity3 implements Activity<SampleStates> {

    //     @Override
    //     public void modelEffects(SimulationContext<SampleStates> ctx, SampleStates states) {
    //         System.out.println("\tActivity3 effect model - part 1!");
    //         states.stateA.setValue(3.1);
    //         ctx.delay(new Duration(1 * Duration.ONE_SECOND));

    //         System.out.println("\tActivity3 effect model - part 2!");
    //         states.stateA.setValue(3.2);
    //         ctx.delay(new Duration(10 * Duration.ONE_DAY));

    //         System.out.println("\tActivity3 effect model - part 3!");
    //         states.stateA.setValue(3.3);
    //     }

    // }

    // public class Activity1A implements Activity<SampleStates> {

    //     @Override
    //     public void modelEffects(SimulationContext<SampleStates> ctx, SampleStates states) {
    //         System.out.println("\tActivity1A effect model!");
    //         states.stateB.setValue("1A");
    //         ctx.delay(new Duration(1 * Duration.ONE_SECOND));
    //     }
    // }

    // public class Activity1B implements Activity<SampleStates> {

    //     @Override
    //     public void modelEffects(SimulationContext<SampleStates> ctx, SampleStates states) {
    //         System.out.println("\tActivity1B effect model!");
    //         states.stateB.setValue("1B");
    //         ctx.delay(new Duration(5 * Duration.ONE_SECOND));
    //     }
    // }

    // @Test
    // public void sequentialSimulation() {
    //     Time simStart = new Time();

    //     ActivityThread<SampleStates> act1Thread = new ActivityThread<>(
    //         new Activity1(), simStart.add(new Duration(Duration.ONE_DAY))
    //     );
    //     ActivityThread<SampleStates> act2Thread = new ActivityThread<>(
    //         new Activity2(), simStart.add(new Duration(2 * Duration.ONE_DAY))
    //     );
    //     ActivityThread<SampleStates> act3Thread = new ActivityThread<>(
    //         new Activity3(), simStart.add(new Duration(3 * Duration.ONE_DAY))
    //     );

    //     List<ActivityThread<SampleStates>> actList = new ArrayList<>();
    //     actList.add(act1Thread);
    //     actList.add(act2Thread);
    //     actList.add(act3Thread);

    //     System.out.println("======================= INPUT PLAN ======================");
    //     for (ActivityThread<SampleStates> thread : actList) {
    //         System.out.println("\tActivity: " + thread.toString() + " | Start Time: " + thread.getEventTime());
    //     }

    //     SampleStates states = new SampleStates();

    //     SimulationEngine<SampleStates> engine = new SimulationEngine<>(simStart, actList, states);
    //     System.out.println("=================== SIMULATION START ====================\n");
    //     engine.simulate();
    //     System.out.println("================== SIMULATION COMPLETE ==================");
    //     System.out.println("=========================================================");

    //     System.out.println("StateA History:");
    //     for (Map.Entry<Time, ?> entry : states.stateA.getHistory().entrySet()) {
    //         System.out.println("\tTime: " + entry.getKey().toString() + " | Value: " + entry.getValue().toString());
    //     }

    //     System.out.println("StateB History:");
    //     for (Map.Entry<Time, ?> entry : states.stateB.getHistory().entrySet()) {
    //         System.out.println("\tTime: " + entry.getKey().toString() + " | Value: " + entry.getValue().toString());
    //     }
    // }

    public class DiverseStates implements StateContainer {
        public final SettableState<Double> floatState = new SettableState<>();
        public final SettableState<String> stringState = new SettableState<>();
        public final SettableState<List<Double>> arrayState = new SettableState<>();
        public final SettableState<Boolean> booleanState = new SettableState<>();

        public List<SettableState<?>> getStateList() {
            return List.of(floatState, stringState, arrayState, booleanState);
        }
    }

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

    @Test
    public void durationTest() {
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