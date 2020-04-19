package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit.MICROSECONDS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import org.junit.Test;

public final class SimulationEngineTests {
    @Test
    public void testChildrenOrdering() {
        final var simEngine = new SimulationEngine();

        final var expectedTrace = List.of(
            "root-1 " + simEngine.getCurrentTime().plus(0, MICROSECONDS),
            "root-2 " + simEngine.getCurrentTime().plus(0, MICROSECONDS),
            "child[0]-1 " + simEngine.getCurrentTime().plus(1, MICROSECONDS),
            "child[1]-1 " + simEngine.getCurrentTime().plus(2, MICROSECONDS),
            "child[2]-1 " + simEngine.getCurrentTime().plus(3, MICROSECONDS)
        );

        final var trace = new ArrayList<>();
        simEngine.scheduleJobAfter(Duration.ZERO, (root) -> {
            trace.add("root-1 " + root.now());

            root.defer(Duration.of(3, MICROSECONDS), (child) -> trace.add("child[2]-1 " + child.now()));
            root.defer(Duration.of(1, MICROSECONDS), (child) -> trace.add("child[0]-1 " + child.now()));
            root.defer(Duration.of(2, MICROSECONDS), (child) -> trace.add("child[1]-1 " + child.now()));

            trace.add("root-2 " + root.now());
        });

        simEngine.runToCompletion();

        assertEquals(expectedTrace, trace);
    }

    /**
     * Tests the functionality of delays within effect models
     */
    @Test
    public void testDelay() {
        final var simEngine = new SimulationEngine();

        final var expectedTrace = List.of(
            "root-1 " + simEngine.getCurrentTime().plus(0, MICROSECONDS),
            "root-2 " + simEngine.getCurrentTime().plus(3, MICROSECONDS),
            "root-3 " + simEngine.getCurrentTime().plus(5, MICROSECONDS)
        );

        final var trace = new ArrayList<>();
        simEngine.scheduleJobAfter(Duration.ZERO, (root) -> {
            trace.add("root-1 " + root.now());

            root.delay(Duration.of(3, MICROSECONDS));
            trace.add("root-2 " + root.now());

            root.delay(Duration.of(2, MICROSECONDS));
            trace.add("root-3 " + root.now());
        });

        simEngine.runToCompletion();

        assertEquals(expectedTrace, trace);
    }

    @Test
    public void testDeferredChild() {
        final var simEngine = new SimulationEngine();

        final var expectedTrace = List.of(
            "root-1 "  + simEngine.getCurrentTime().plus(0, MICROSECONDS),
            "root-2 "  + simEngine.getCurrentTime().plus(0, MICROSECONDS),
            "child-1 " + simEngine.getCurrentTime().plus(3, MICROSECONDS),
            "child-2 " + simEngine.getCurrentTime().plus(5, MICROSECONDS)
        );

        final var trace = new ArrayList<>();
        simEngine.scheduleJobAfter(Duration.ZERO, (root) -> {
            trace.add("root-1 " + root.now());

            root.defer(Duration.of(3, MICROSECONDS), (child) -> {
                trace.add("child-1 " + child.now());

                child.delay(Duration.of(2, MICROSECONDS));
                trace.add("child-2 " + child.now());
            });
            trace.add("root-2 " + root.now());
        });

        simEngine.runToCompletion();

        assertEquals(expectedTrace, trace);
    }

    @Test
    public void testAwaitBlocksOnChild() {
        final var simEngine = new SimulationEngine();

        final var expectedTrace = List.of(
            "root-1 "  + simEngine.getCurrentTime().plus(0, MICROSECONDS),
            "root-2 "  + simEngine.getCurrentTime().plus(0, MICROSECONDS),
            "child-1 " + simEngine.getCurrentTime().plus(3, MICROSECONDS),
            "child-2 " + simEngine.getCurrentTime().plus(5, MICROSECONDS),
            "root-3 "  + simEngine.getCurrentTime().plus(5, MICROSECONDS)
        );

        final var trace = new ArrayList<>();
        simEngine.scheduleJobAfter(Duration.ZERO, (root) -> {
            trace.add("root-1 " + root.now());

            final var childHandle = root.defer(Duration.of(3, MICROSECONDS), (child) -> {
                trace.add("child-1 " + child.now());

                child.delay(Duration.of(2, MICROSECONDS));
                trace.add("child-2 " + child.now());
            });
            trace.add("root-2 " + root.now());

            childHandle.await();
            trace.add("root-3 " + root.now());
        });

        simEngine.runToCompletion();

        assertEquals(expectedTrace, trace);
    }
}
