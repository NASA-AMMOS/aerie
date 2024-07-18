package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface SimulationResultsHandle {
  SimulationDatasetId getSimulationDatasetId();

  Instant startTime();

  Duration duration();

  SimulationResults getSimulationResults();

  ProfileSet getProfiles(final List<String> profileNames);

  Map<ActivityInstanceId, ActivityInstance> getSimulatedActivities();
}
