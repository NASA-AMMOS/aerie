package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationResultsHandle {

  private final SimulationResults simulationResults;

  public SimulationResultsHandle(final SimulationResults simulationResults) {
    this.simulationResults = simulationResults;
  }

  public SimulationResults getSimulationResults() {
    return this.simulationResults;
  }

  public ProfileSet getProfiles(final Iterable<String> profileNames) {
    final var realProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>>();
    for (final var profileName : profileNames) {
      if (this.simulationResults.realProfiles.containsKey(profileName)) {
        realProfiles.put(profileName, this.simulationResults.realProfiles.get(profileName));
      } else if (this.simulationResults.discreteProfiles.containsKey(profileName)) {
        discreteProfiles.put(profileName, this.simulationResults.discreteProfiles.get(profileName));
      }
    }
    return ProfileSet.of(realProfiles, discreteProfiles);
  }

  public Map<SimulatedActivityId, SimulatedActivity> getSimulatedActivities() {
    return this.simulationResults.simulatedActivities;
  }
}
