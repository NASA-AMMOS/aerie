package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.ArrayList;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit.MICROSECONDS;

public final class ExampleSimulation {
    /// Trace a simulation with 100 tasks, each spawning 1 sub-task.
    public static void main(final String[] args) {
        final var simEngine = new SimulationEngine();

        final var trace = new ArrayList<>();
        simEngine.scheduleJobAfter(Duration.ZERO, (root) -> {
            for (int i = 0; i < 100; i++) {
                final var index = i;

                root.defer(Duration.ZERO, (parent) -> {
                    root.delay(Duration.of(100 * (index + 1), MICROSECONDS));
                    trace.add(parent.now() + "\t" + "parent[" + index + "]-1");

                    parent.delay(Duration.of(10, MICROSECONDS));
                    trace.add(parent.now() + "\t" + "parent[" + index + "]-2");

                    parent.defer(Duration.ZERO, (child) -> {
                        trace.add(child.now() + "\t" + "child[" + index + "]-1");

                        child.delay(Duration.of(1, MICROSECONDS));
                        trace.add(child.now() + "\t" + "child[" + index + "]-2");
                    });

                    parent.delay(Duration.of(10, MICROSECONDS));
                    trace.add(parent.now() + "\t" + "parent[" + index + "]-3");
                });
            }
        });

        simEngine.runToCompletion();

        trace.forEach(System.out::println);
    }
}
