package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.classes.CustomEnums.InstrumentMode;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

public class Runsim {
    public static void runSimulation(Config config) {
        List<ActivityJob<?>> plan = Plan.createPlan(config);
        SampleMissionStates sampleMissionStates = new SampleMissionStates(config);

        SimulationEngine engine = new SimulationEngine(config.missionStartTime, plan, sampleMissionStates);
        engine.simulate();

        // note that this currently doesn't have the initial value since that isn't stored in the history
        Map<Time, InstrumentMode> modeHistory = sampleMissionStates.instrumentMode.getHistory();
        
        for (Map.Entry<Time, InstrumentMode> entry : modeHistory.entrySet()) {
            System.out.println("Time: " + entry.getKey() + " | Mode: " + entry.getValue());
        }
    }
}