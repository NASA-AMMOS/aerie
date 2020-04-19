package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.withEffects;

public class Runsim {
    public static void runSimulation(Config config) {
        Geometry.loadSpiceAndKernels();

        final var simEngine = new SimulationEngine();
        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> Plan.runPlan(config)));

        final var model = new Model(simEngine.getCurrentTime());
        SampleMissionStates.useModelsIn(model, simEngine::runToCompletion);
    }
}
