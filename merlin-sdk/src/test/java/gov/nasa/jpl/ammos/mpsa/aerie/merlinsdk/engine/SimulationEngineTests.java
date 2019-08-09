package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationEngineTests {

    public class SampleStates {

    }

    public class Activity1 implements Activity<SampleStates> {

        @Override
        // public void modelEffects(SimulationContext ctx, SampleStates states) {
        public void modelEffects(SimulationContext ctx) {
            System.out.println("\tActivity1 effect model - part 1!");
            ctx.delay(new Duration(4 * Duration.ONE_DAY));

            Activity1A child = new Activity1A();
            System.out.println("\tSpawning child Activity1A in parallel!");
            ctx.spawnActivity(child);

            System.out.println("\tActivity1 effect model - part 2!");
            ctx.delay(new Duration(2 * Duration.ONE_DAY));
            System.out.println("\tActivity1 effect model - part 3!");
        }

    }

    public class Activity2 implements Activity<SampleStates> {

        @Override
        // public void modelEffects(SimulationContext ctx, SampleStates states) {
        public void modelEffects(SimulationContext ctx) {
            System.out.println("\tActivity2 effect model - part 1!");
            ctx.delay(new Duration(1 * Duration.ONE_DAY));
            System.out.println("\tActivity2 effect model - part 2!");
        }
        
    }

    public class Activity3 implements Activity<SampleStates> {

        @Override
        // public void modelEffects(SimulationContext ctx, SampleStates states) {
        public void modelEffects(SimulationContext ctx) {
            System.out.println("\tActivity3 effect model - part 1!");
            ctx.delay(new Duration(1 * Duration.ONE_SECOND));
            System.out.println("\tActivity3 effect model - part 2!");
            ctx.delay(new Duration(10 * Duration.ONE_DAY));
            System.out.println("\tActivity3 effect model - part 3!");
        }

    }

    public class Activity1A implements Activity<SampleStates> {

        @Override
        public void modelEffects(SimulationContext ctx) {
            System.out.println("\tActivity1A effect model!");
            ctx.delay(new Duration(1 * Duration.ONE_SECOND));
        }
    }

    @Test
    public void sequentialSimulation() {
        Time simStart = new Time();

        ActivityThread act1Thread = new ActivityThread(
            new Activity1(), simStart.add(new Duration(Duration.ONE_DAY))
        );
        ActivityThread act2Thread = new ActivityThread(
            new Activity2(), simStart.add(new Duration(2 * Duration.ONE_DAY))
        );
        ActivityThread act3Thread = new ActivityThread(
            new Activity3(), simStart.add(new Duration(3 * Duration.ONE_DAY))
        );

        List<ActivityThread> actList = new ArrayList<>();
        actList.add(act1Thread);
        actList.add(act2Thread);
        actList.add(act3Thread);
        SimulationEngine engine = new SimulationEngine(simStart, actList);
        engine.simulate();
    }

}