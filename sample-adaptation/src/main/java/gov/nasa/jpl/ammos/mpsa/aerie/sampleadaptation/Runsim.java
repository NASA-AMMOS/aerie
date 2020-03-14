package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

public class Runsim {
    public static void runSimulation(Config config) {
        Geometry.loadSpiceAndKernels();

        final var simStartTime = SimulationInstant.ORIGIN;
        final var sampleMissionStates = new SampleMissionStates(config);

        SimulationEngine.simulate(simStartTime, sampleMissionStates, () -> {
            Plan.runPlan(config, simStartTime);
        });

        // note that this currently doesn't have the initial value since that isn't stored in the history
        for (final var entry : sampleMissionStates.instrumentMode.getHistory().entrySet()) {
            System.out.println("Time: " + entry.getKey() + " | Mode: " + entry.getValue());
        }
    }
}