package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.Collections;

public class Runsim {
    public static void runSimulation(Config config) {
        Geometry.loadSpiceAndKernels();

        final var simStartTime = SimulationInstant.ORIGIN;
        final var model = new Model();

        SampleMissionStates.useModelsIn(model, () -> {
            SimulationEngine.simulate(simStartTime, Collections.emptyList(), () -> {
                Plan.runPlan(config, simStartTime);
            });
        });
    }
}