package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.classes.CustomEnums.InstrumentMode;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

public class Runsim {
    public static void runSimulation(Config config) {
        final Instant simStartTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

        final var plan = Plan.createPlan(config, simStartTime);
        SampleMissionStates sampleMissionStates = new SampleMissionStates(config);

        SimulationEngine.simulate(simStartTime, plan, sampleMissionStates);

        // note that this currently doesn't have the initial value since that isn't stored in the history
        Map<Instant, InstrumentMode> modeHistory = sampleMissionStates.instrumentMode.getHistory();
        
        for (Map.Entry<Instant, InstrumentMode> entry : modeHistory.entrySet()) {
            System.out.println("Time: " + entry.getKey() + " | Mode: " + entry.getValue());
        }
    }
}